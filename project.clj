(defproject auto_parallel "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/tools.trace "0.7.9"]
                 [org.clojure/clojure "1.7.0"]
                 [com.climate/claypoole "1.1.0"]]
  :main ^:skip-aot auto-parallel.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
