(ns benchmark.core
  (:use benchmark.util)
  (:use benchmark.search)
  (:use benchmark.fib)
  (:require [criterium.core :as cr]))

(def live-benchmarks (atom []))

(defn add-benchmarks [which]
  (swap!
    live-benchmarks
    #(cons which %)))

;; should a suite know how to run itself, or is this probably sufficient?
(defn run-suite [s]
  (println "running suite" s)
  (doseq [b (benchmarks s)] (b)))

(defn run-benchmarks []
  (doseq [s @live-benchmarks]
    (run-suite s)))

(defn -main [& args]
  ; (add-benchmarks search-benchmarks)
  ; (add-benchmarks large-fib-benchmarks)
  (add-benchmarks small-fib-benchmarks)

  (println "Benchmark starting")
  (run-benchmarks))
