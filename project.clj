(defproject auto_parallel "0.1.0-SNAPSHOT"

  :dependencies [[org.clojure/tools.trace "0.7.9"]
                 [org.clojure/clojure "1.8.0"]
                 [com.google.caliper/caliper "0.5-rc1"]
                 [criterium "0.4.3"]]

  :main ^:skip-aot com.dpzmick.parallel-macros

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}
             :benchmark {:aot :all
                         :jvm-opts ["-server"]}}

  :aliases {"benchmark" ["with-profile" "benchmark" "run" "-m" "benchmark.core"]
            "jbenchmark" ["with-profile" "benchmark" "run" "-m" "com.dpzmick.auto_parallel_java.App"]
            "slamhound" ["run" "-m" "slam.hound"]}

  :jvm-opts ["-server"]

  :java-source-paths ["src_java/"]

  :plugins [[lein-cloverage "1.0.2"]])
