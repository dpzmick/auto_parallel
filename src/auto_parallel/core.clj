(ns auto-parallel.core
  (use [clojure.tools.trace])
  (use [auto-parallel.util])
  (:require [com.climate.claypoole :as cp]))

;; make it easier to play with syntax tree
(defn args [expr] (rest expr))
(defn fun  [expr] (first expr))
(defn const? [expr] (not (seq? expr)))
(defn all-args-const?
  [expr]
  (every? true? (map const? (args expr))))

; https://gist.github.com/jcromartie/5459350
(defmacro parlet
  [pool bindings & forms]
  (let
    [pairs (partition 2 bindings)
     names (map first pairs)
     vals  (map second pairs)]
    `(let [[~@names] (cp/pvalues ~pool ~@vals)] ~@forms)))

;; everything this recovers in :names can be computed in parallel
;; call this multiple times to get multiple parallelize able steps
;; assumes everything is a function call
(defn prune
  "prunes a single level of leaves from syntax tree"
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
  [pool expr]
  (if (const? expr)
    expr
    (let
      [{e :expr n :names} (prune expr)
       bindings           (apply vector (apply concat (into (list) n)))]
      `(parlet ~pool ~bindings ~(make-nested-lets pool e)))))

(defmacro parexpr [pool expr] (make-nested-lets pool expr))
