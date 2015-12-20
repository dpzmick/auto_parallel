(ns benchmark.util
  (:require [criterium.core :as cr]))

(defprotocol BenchmarkSuite
  (benchmarks [this]))

(defmacro defb [n e]
  `(defn ~n []
     (println ~n)
     (cr/bench ~e)))

