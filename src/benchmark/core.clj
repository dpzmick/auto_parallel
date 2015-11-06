(ns benchmark.core
  (:require [criterium.core :as cr]))

(def loud true)

(defmacro b [e]
  (if loud
    `(cr/with-progress-reporting (cr/bench ~e :verbose))
    `(cr/bench ~e)))

(defn is-it-working? []
  (b (Thread/sleep 500)))

(defn -main [& args]
  (println "Benchmark starting")
  (is-it-working?))
