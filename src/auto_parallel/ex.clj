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
