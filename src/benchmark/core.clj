(ns benchmark.core
  (:use benchmark.fib)
  (:use benchmark.merge)
  (:use benchmark.pmap)
  (:use benchmark.id3)
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

(def fib-benchmarks [
                     fib-parexpr-bench
                     fib-future-bench
                     ])

;; merge
(def n-merge 50000)
(def merge-list (take n-merge (repeatedly #(rand-int n-merge))))
(defb merge-parexpr (merge-sort-parexpr pool merge-list))
(defb merge-futures (merge-sort-futures merge-list))

(def merge-benchmarks [
                       merge-parexpr
                       ;merge-futures ;; this isn't useful, runs out of memory
                       ])

;; pmap test
(def n-pmap 200)
(def list-pmap (take n-pmap (repeatedly #(rand-int 1000))))
(defb fj-pmap-bench (fj-pmap pool list-pmap))
(defb builtin-pmap-bench (builtin-pmap list-pmap))

(def pmap-bechmarks [
                     fj-pmap-bench
                     builtin-pmap-bench
                     ])

;; only testing on entropy function
(def n 10000)
(def m 10000)
(def entropy-attrs (make-attrs m))
(def entropy-data (make-random-data n entropy-attrs))

;; if we define these here, this will not work, clojure will cache the filter
;; operations (or something like that)

(defb entropy-stock-bench (entropy entropy-data (first entropy-attrs)))
(defb entropy-par-bench (entropy-par pool entropy-data (first entropy-attrs)))

(def entropy-benchmarks [
                         entropy-stock-bench
                         entropy-par-bench
                         ])

;; drive it
; (def benchmark-sets-to-run [fib-benchmarks])
; (def benchmark-sets-to-run [merge-benchmarks])
; (def benchmark-sets-to-run [pmap-bechmarks])
(def benchmark-sets-to-run [entropy-benchmarks])

(defn -main [& args]
  (println "Benchmark starting")
  (doseq [s benchmark-sets-to-run] (run-set s)))
