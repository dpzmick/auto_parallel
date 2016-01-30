(ns auto-parallel.util
  (:require [clojure.pprint :refer :all]
            [clojure.walk :refer :all]))

(defn any-true? [lst] (not (nil? (some true? lst))))

(defn iterative-list-accum
  ([lst] (iterative-list-accum lst [(list)]))
  ([lst acc]
   (if (empty? lst)
     acc
     (recur (rest lst) (conj acc (cons (first lst) (last acc)))))))

(defn slow-function
  "returns a function which waits for a while, then returns the result of the no
  argument function"
  [how-slow fun]
  (fn []
    (do
      (Thread/sleep how-slow)
      (fun))))

(defn macropprint   [expr] (pprint (macroexpand-all expr)))
(defn macropprint-1 [expr] (pprint (macroexpand-1   expr)))

(defn macropprint-steps [expr]
  (println "STEP STARTS")
  (pprint expr)
  (let [form (macroexpand-1 expr)]
    (when-not (= form expr)
      (recur form))))
