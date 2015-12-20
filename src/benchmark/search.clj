(ns benchmark.search
  (:use benchmark.util)
  (:require [auto-parallel.core :as ap]))

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
(def n-search 100)
(def search-list (doall (take n-search (repeatedly #(rand-int n-search)))))

(defb search-par-bench (search-par 1 search-list))
(defb search-serial-bench (search-serial 1 search-list))

;; define the benchmark suite exposed by this namespace
(def search-benchmarks
  (reify BenchmarkSuite
    (toString [this] "search benchmarks")
    (benchmarks [this]
      [search-par-bench search-serial-bench])))
