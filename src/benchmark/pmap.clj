(ns benchmark.pmap
  (:require [auto-parallel.fork-join-par :as ap]))

(defn prime? [n]
  (every? #(pos? (rem n %)) (range 2 (Math/sqrt (inc n)))))

(defn nth-prime [n]
  (loop [i 0 d 1]
    (if (prime? d)
      (if (= i n)
        d
        (recur (+ 1 i) (+ 1 d)))
      (recur i (+ 1 d)))))

;; determine which pmap performs better for a long running function
(defn fj-pmap [pool lst]
  (doall (ap/pmap pool nth-prime lst)))

(defn builtin-pmap [lst]
  (doall (pmap nth-prime lst)))
