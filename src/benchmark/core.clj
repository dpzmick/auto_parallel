(ns benchmark.core
  (:use benchmark.fib)
  (:require [criterium.core :as cr]))

(import 'java.util.concurrent.ForkJoinPool)

(def loud false)

(defmacro defb [n e]
  `(defn ~n []
     (let [~'pool (ForkJoinPool.)]
       (println ~n)
       ~(if loud
          `(cr/with-progress-reporting (cr/bench ~e :verbose))
          `(cr/bench ~e)))))

(defn run-set [s] (doseq [b s] (b)))

(defb is-it-working? (Thread/sleep 500))

;; fib
;; run out of memory before anything interesting happens, with the future
;; benchmark
(def n-fib 15)
(defb fib-parexpr-bench (fib-parexpr pool n-fib))
(defb fib-future-bench  (fib-future n-fib))

;; drive it
(def fib-benchmarks [fib-parexpr-bench fib-future-bench])
(def benchmark-sets-to-run [fib-benchmarks])

(defn -main [& args]
  (println "Benchmark starting")
  (doseq [s benchmark-sets-to-run] (run-set s)))
