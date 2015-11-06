(defproject auto_parallel "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/tools.trace "0.7.9"]
                 [org.clojure/clojure "1.7.0"]
                 [criterium "0.4.3"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [org.clojure/math.combinatorics "0.1.0"]
                 [com.climate/claypoole "1.1.0"]]
  :main ^:skip-aot auto-parallel.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :benchmark {:aot :all
                         :jvm-opts ["-server"]}}
  :aliases {"benchmark" ["with-profile" "benchmark" "run" "-m" "benchmark.core"]})
