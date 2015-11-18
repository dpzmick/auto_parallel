(ns auto-parallel.core
  (use [clojure.tools.trace])
  (use [auto-parallel.util])
  (:require [auto-parallel.fork-join-par :as p]))

;; TODO handle map syntax, then I think I will have hit all of the syntax needed
;; to be complete

;; some utilities
(defn any-true? [lst] (not (nil? (some true? lst))))

(defn iterative-list-accum
  ([lst] (iterative-list-accum lst [(list)]))
  ([lst acc]
   (if (empty? lst)
     acc
     (recur (rest lst) (conj acc (cons (first lst) (last acc)))))))

;; check for dependencies
(def dep-in-expr?) ;; defined later

;; most binding forms expand to a let*, so do the macroexpand first
(defn dependency?  [var-name expr] (dep-in-expr? var-name (macroexpand expr)))
(defn depend-on-any? [names expr] (any-true? (map #(dependency? % expr) names)))

(defn dep-in-let?
  ([var-name expr] (dep-in-let? var-name (partition 2 (second expr)) (rest (rest expr))))

  ([var-name bindings forms]
   (if (empty? bindings)
     ;; check in all the sub forms of the let expression
     (any-true? (map #(dependency? var-name % ) forms))

     ;; we still have some bindings to evaluate
     (let
       [first-name  (first  (first bindings))
        first-value (second (first bindings))]

       ;; if we rebind the value, there is no dependency in anything
       ;; inheriting this environment, just need to check that the binding
       ;; does not use the variable in question
       (if (= first-name var-name)
         (dependency? var-name first-value)

         ;; otherwise, the binding may still depend on the name, or the rest of
         ;; the bindings might depend on the name
         (or (dependency? var-name first-value)
             (dep-in-let? var-name (rest bindings) forms)))))))

(defn dep-in-normal-expr?  [var-name expr] (any-true? (map #(dependency? var-name %) expr)))
(defn dep-in-const?        [var-name expr] (= var-name expr))

(defn dep-in-expr?
  [var-name expr]
  (if (sequential? expr)

    ;; we have a function call or some other form (like a vector or list
    ;; literal)
    (if (= 'let* (first expr))
      (dep-in-let? var-name expr)
      (dep-in-normal-expr? var-name expr))

    ;; the expression is a single term
    (dep-in-const? var-name expr)))

;; replace-all
(def replace-in-expr) ;; defined later

;; most binding forms expand to a let*, so do the macroexpand first
(defn replace-all    [e replacement expr] (replace-in-expr e replacement (macroexpand expr)))
(defn replace-many   [es reps expr]
  (if (empty? es)
    expr
    (recur
      (rest es)
      (rest reps)
      (replace-all (first es) (first reps) expr))))

(defn replace-in-let
  ([e replacment expr] (replace-in-let e replacment (partition 2 (second expr)) (rest (rest expr))))

  ([e replacment bindings forms]
   (if (= 1 (count bindings))
     ;; will only ever happen inside of the recursive call for the let
     ;; expression when we still need to perform replacements in this env
     (let
       [first-name  (first  (first bindings))
        first-value (second (first bindings))
        replaced-v  (replace-all e replacment first-value)]
       `(let
          [~first-name ~replaced-v]
          ~@(map #(replace-all e replacment %) forms)))

     ;; we still have some bindings to evaluate
     (let
       [first-name  (first  (first bindings))
        first-value (second (first bindings))
        replaced-v  (replace-all e replacment first-value)]

       ;; if we rebind the value, there is no dependency in anything
       ;; inheriting this environment, just need to perform replacement in the
       ;; binding, then emit the rest of the expression as is
       (if (= first-name e)
         `(let
            [~first-name ~replaced-v]
            (let
              ~(apply vector (flatten (rest bindings)))
              ~@forms))

         ;; otherwise, the binding may still depend on the name, some other
         ;; binding might, or the body might
         `(let [~first-name ~replaced-v] ~(replace-in-let e replacment (rest bindings) forms)))))))

(defn replace-in-normal-expr [e replacement expr] (map #(replace-all e replacement %) expr))
(defn replace-in-const       [e replacement expr] (if  (= e expr) replacement expr))

(defn replace-in-expr
  [e replacement expr]
  (if (sequential? expr)

    ;; we have a function call or some other form (like a vector or list
    ;; literal)
    (if (= 'let* (first expr))
      (replace-in-let e replacement expr)
      (replace-in-normal-expr e replacement expr))

    ;; the expression is a single term
    (replace-in-const e replacement expr)))

;; parlet
(defn let-has-deps? [bindings]
  (let
    [pairs          (partition 2 bindings)
     names          (map first pairs)
     exprs          (map second pairs)
     relevant-names (butlast (iterative-list-accum names))]
    (any-true? (map depend-on-any? relevant-names exprs))))

(defmacro parlet
  [bindings & forms]
  (if (let-has-deps? bindings)
    (throw (Exception. "this let form cannot be parallel. There are dependencies in the bindings"))
    (let
      [pairs    (partition 2 bindings)
       names    (map first pairs)
       vals     (map second pairs)
       tasks    (apply vector (map (fn [v] `(p/fork (p/new-task (fn [] ~v)))) vals))
       new-vals (map (fn [n] `(p/join ~n)) names)]

      ;; each val becomes a fork join task, each reference to the value becomes
      ;; a (join task)

      ;; use pattern matching to express this
      `(let [[~@names] ~tasks] ~@(map #(replace-many names new-vals %) forms)))))


;; parexpr
;; make it easier to play with syntax tree
(defn args [expr] (rest expr))
(defn fun  [expr] (first expr))
(defn const? [expr] (not (seq? expr)))
(defn all-args-const?
  [expr]
  (every? true? (map const? (args expr))))

(defn prune
  "
  finds 'leaves' in the syntax tree. Everything in :names can be computed in
  parallel
  "
  [expr]
  (cond
    (const? expr)
    {:expr expr, :names {}}

    (all-args-const? expr)
    (let
      [n (gensym "expr")]
      {:expr n, :names {n expr}})

    :else
    (let
      [subresults (map prune (args expr))
       subexprs   (map :expr subresults)
       subnames   (map :names subresults)
       mynames    (apply merge subnames)]
      {:expr (cons (fun expr) subexprs), :names mynames})))

(defn make-nested-lets
  "
  makes nested parlets such that parallelism is maximized
  "
  [expr]
  (if (const? expr)
    expr
    (let
      [{e :expr n :names} (prune expr)
       bindings           (apply vector (apply concat (into (list) n)))]
      `(parlet ~bindings ~(make-nested-lets e)))))

(defmacro parexpr [expr] (make-nested-lets expr))

; ;; TODO deal with maps
; (defn replace-all
;   "
;   replace all occurrences of the subexpression to-replace with the subexpression
;   replace-with in the expression expr
;   "
;   [expr to-replace replace-with]
;   (cond
;     (= expr to-replace) replace-with
;     (sequential? expr) (map #(replace-all % to-replace replace-with) expr)
;     :else expr))
