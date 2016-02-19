(ns benchmark.search
  (:require [com.dpzmick.parallel-macros.defparfun :refer [defparfun]]))

;; getting this to compile too some doing
(defparfun search-par [value lst]
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
