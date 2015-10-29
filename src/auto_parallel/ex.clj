(ns auto-parallel.ex
  (:require [auto-parallel.core :as ap]
            [clojure.math.numeric-tower :as math]
            [clojure.math.combinatorics :refer [selections]]))

(import 'java.util.concurrent.ForkJoinPool)
(def pool (ForkJoinPool.))

;; a really bad Fibonacci code, but uses parexpr to evaluate.
;; when run this will full saturate the CPU, because each parexpr will get
;; evaluated in a fork/join fashion. This will very quickly create more tasks
;; than there are threads, and will give us parallelism
(defn fib
  [n]
  (if (or (= 0 n) (= 1 n))
    1
    (ap/parexpr pool
                (+
                 (fib (- n 1))
                 (fib (- n 2))))))

(defn merge-seqs
  "Merges two sorted sequences into a single sorted sequence"
  ([left right]
    (merge-seqs (list left right)))
  ([[left right]]
    (loop [l left, r right, result []]
      (let [lhead (first l), rhead (first r)]
        (cond
          (nil? lhead)     (concat result r)
          (nil? rhead)     (concat result l)
          (<= lhead rhead) (recur (rest l) r (conj result lhead))
          true             (recur l (rest r) (conj result rhead)))))))

(defn merge-sort
  [lst]
  (if (= 1 (count lst))
    lst

    (let
      [middle (/ (count lst) 2)
       front  (take middle lst)
       back   (drop middle lst)]
      (ap/parexpr pool (merge-seqs (merge-sort front) (merge-sort back))))))
