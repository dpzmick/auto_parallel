(ns benchmark.fib
  (:require [auto-parallel.core :as ap]))

;; a really bad Fibonacci code, but uses parexpr to evaluate.
;; when run this will full saturate the CPU, because each parexpr will get
;; evaluated in a fork/join fashion. This will very quickly create more tasks
;; than there are threads, and will give us parallelism
(defn fib-parexpr
  [pool n]
  (if (or (= 0 n) (= 1 n))
    1
    (ap/parexpr pool (+ (fib-parexpr pool (- n 1))
                        (fib-parexpr pool (- n 2))))))

(defn fib-future [n]
  (if (or (= 0 n) (= 1 n))
    1
    (let [a (future (fib-future (- n 1)))
          b (future (fib-future (- n 2)))]
      (+ @a @b))))
