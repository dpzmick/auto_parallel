(ns benchmark.search
  (:use benchmark.util)
  (:require [auto-parallel.core :as ap]))

;; getting this to compile too some doing
(ap/defparfun search-par [value lst]
  (cond
    (> 1 (count lst)) false
    (= 1 (count lst)) (= value (first lst))
    :else             (let [mid (/ (count lst) 2)
                            a   (take mid lst)
                            b   (drop mid lst)]
                        (if (= value (nth lst mid))
                          true
                          (or (search-par value a) (search-par value b))))))

(defn search-serial [value lst]
  (cond
    (> 1 (count lst)) false
    (= 1 (count lst)) (= value (first lst))
    :else             (let [mid (/ (count lst) 2)
                            a   (take mid lst)
                            b   (drop mid lst)]
                        (if (= value (nth lst mid))
                          true
                          (or (search-serial value a) (search-serial value b))))))

;; define all of the possible benchmarks for this namespace
(def n-search 1000000)
(def search-list (vec (take n-search (repeatedly #(rand-int n-search)))))

(defb search-par-bench (search-par 1 search-list))
(defb search-serial-bench (search-serial 1 search-list))

;; define the benchmark suite exposed by this namespace
(def search-benchmarks
  (make-benchmark-suite
    "search-benchmarks"
    [search-par-bench search-serial-bench]))
