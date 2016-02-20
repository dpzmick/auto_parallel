(ns com.dpzmick.parallel-macros.parexpr
  (:require [com.dpzmick.ast-manip.dependency :refer :all]
            [com.dpzmick.ast-manip.replace :refer :all]
            [com.dpzmick.runtime.fork-join-par :as p]
            [com.dpzmick.parallel-macros.parlet :refer [parlet]]
            [com.dpzmick.util :refer :all]))
;; parexpr
;; make it easier to play with syntax tree
(defn- args [expr] (rest expr))
(defn- fun  [expr] (first expr))
(defn- const? [expr] (not (seq? expr)))
(defn- all-args-const?
  [expr]
  (every? true? (map const? (args expr))))

(defn- prune
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

(defn- make-nested-lets
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

;; TODO need to detect when one of the parlets is singular and remove it.
;; it isn't the case that a parlet with a single binding should be a noop in
;; general, so we can't change parlet to make this hold
(defmacro parexpr [expr] (make-nested-lets expr))
