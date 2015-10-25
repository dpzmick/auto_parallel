(ns auto-parallel.util
  (use [clojure.walk])
  (use [clojure.pprint]))

(defn slow-function
  "returns a function which waits for a while, then returns the result of the no argument function"
  [how-slow fun]
  (fn []
    (do
      (Thread/sleep how-slow)
      (fun))))

(defn macropprint   [expr] (pprint (macroexpand-all expr)))
(defn macropprint-1 [expr] (pprint (macroexpand-1   expr)))
