(ns auto-parallel.core
  (use [clojure.tools.trace])
  (use [auto-parallel.util])
  (use [auto-parallel.replace])
  (use [auto-parallel.dependency])
  (:require [auto-parallel.fork-join-par :as p]))

;; TODO handle map syntax, then I think I will have hit all of the syntax needed
;; to be complete

(defmacro parlet
  [bindings & forms]
  (if (let-has-deps? bindings)
    (throw (Exception. "this let form cannot be parallel. There are dependencies in the bindings"))
    (let
      [pairs    (partition 2 bindings)
       names    (map first pairs)
       vals     (map second pairs)
       tasks    (vec (map (fn [v] `(p/fork (p/new-task (fn [] ~v)))) vals))
       new-vals (map (fn [n] `(p/join ~n)) names)]

      ;; each val becomes a fork join task, each reference to the value becomes
      ;; (join task)

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
       bindings           (vec (apply concat (into (list) n)))]
      `(parlet ~bindings ~(make-nested-lets e)))))

(defmacro parexpr [expr] (make-nested-lets expr))

;; TODO evaluate use of -helper for naming, maybe want to use a new generated
;; name
;; TODO I wonder if making a bunch of copies of the method (via the helper) will
;; hurt performance dramatically (because of extra memory usage)
(defmacro defparfun [method-name formals body]
  `(defn ~method-name ~formals
     (let
       [~(symbol (str method-name "-helper")) (fn ~formals ~body)]
       (p/join (p/fork (p/new-task #(~(symbol (str method-name "-helper")) ~@formals)))))))
