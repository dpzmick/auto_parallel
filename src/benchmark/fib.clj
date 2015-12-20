(ns benchmark.fib
  (:use benchmark.util)
  (:require [auto-parallel.core :as ap]))

;; a really bad Fibonacci code, but uses parexpr to evaluate.
;; when run this will fully saturate the CPU, because each parexpr will get
;; evaluated in a fork/join fashion. This will very quickly create more tasks
;; than there are threads, and will give us parallelism
(defn fib-parexpr
  [n]
  (if (or (= 0 n) (= 1 n))
    1
    (ap/parexpr (+ (fib-parexpr (- n 1))
                   (fib-parexpr (- n 2))))))

(defn fib-parlet [n]
  (if (or (= 0 n) (= 1 n))
    1
    (ap/parlet
      [m1 (fib-parlet (- n 1))
       m2 (fib-parlet (- n 2))]
      (+ m1 m2))))

(ap/defparfun fib-parfun [n]
  (if (or (= 0 n) (= 1 n))
    1
    (+
     (fib-parfun (- n 1))
     (fib-parfun (- n 2)))))

;; we quickly run out of threads with this
(defn fib-future [n]
  (if (or (= 0 n) (= 1 n))
    1
    (let [a (future (fib-future (- n 1)))
          b (future (fib-future (- n 2)))]
      (+ @a @b))))

;; some large benchmarks, to compare the different par* types
(def large-fib 20)
(defb large-parexpr (fib-parexpr large-fib))
(defb large-parlet  (fib-parlet  large-fib))
(defb large-parfun  (fib-parfun  large-fib))

(def large-fib-benchmarks
  (reify BenchmarkSuite
    (toString [this] "large fib benchmarks")
    (benchmarks [this]
      [large-parexpr large-parlet large-parfun])))

(def small-fib 10)
(defb small-parexpr (fib-parexpr small-fib))
(defb small-parlet  (fib-parlet  small-fib))
(defb small-parfun  (fib-parfun  small-fib))
(defb small-futures (fib-future  small-fib))

(def small-fib-benchmarks
  (make-benchmark-suite
    "small fib benchmarks"
    [small-parexpr small-parlet small-parfun small-futures]))
