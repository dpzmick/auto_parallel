(defproject auto_parallel "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/tools.trace "0.7.9"]
                 [org.clojure/clojure "1.8.0"]
                 [criterium "0.4.3"]]

  :main ^:skip-aot com.dpzmick.parallel-macros

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}
             :benchmark {:aot :all
                         :jvm-opts ["-server"]}}

  :aliases {"benchmark" ["with-profile" "benchmark" "run" "-m" "benchmark.core"]
            "slamhound" ["run" "-m" "slam.hound"]}

  :plugins [[lein-cloverage "1.0.2"]])
