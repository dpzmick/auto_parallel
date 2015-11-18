(ns auto-parallel.core
  (use [clojure.tools.trace])
  (use [auto-parallel.util])
  (:require [auto-parallel.fork-join-par :as p]))

;; make it easier to play with syntax tree
(defn args [expr] (rest expr))
(defn fun  [expr] (first expr))
(defn const? [expr] (not (seq? expr)))
(defn all-args-const?
  [expr]
  (every? true? (map const? (args expr))))

;; some utilities
(defn any-true? [lst] (not (nil? (some true? lst))))

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


(defmacro parlet
  [pool bindings & forms]
  (let
    [pairs (partition 2 bindings)
     names (map first pairs)
     vals  (map second pairs)]
    `(let [[~@names] (p/pvalues ~pool ~@vals)] ~@forms))) ;; pattern match

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
  [pool expr]
  (if (const? expr)
    expr
    (let
      [{e :expr n :names} (prune expr)
       bindings           (apply vector (apply concat (into (list) n)))]
      `(parlet ~pool ~bindings ~(make-nested-lets pool e)))))

(defmacro parexpr [pool expr] (make-nested-lets pool expr))

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
