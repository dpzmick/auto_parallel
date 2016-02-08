(ns benchmark.core
  (:require [clojure.java.io :as io])
  (:require [clojure.string :refer [split starts-with? join]])
  (:require [benchmark.fib :refer :all]))

(defn- env-expand [string]
  (if (starts-with? string "$")
    (let
      [var-name (join (rest string))]
      (System/getenv var-name))
    string))

(defn- run-line [line]
  (let
    [[fun-name & args] (split line #",")
     args              (map env-expand args)
     function          (resolve (symbol fun-name))]

    (if (nil? function)
      (do
        (println "function" fun-name "not found"))

      (do
        (println "running" fun-name "with args" args)
        (apply function args)))))

;; reads from a csv file in the form:
;; function_name,arg1,arg1,...
;; and executes each function call: (f1 arg1 arg2) ... for each line in input
;; file
;; the functions must have already been required, because that's easier
;; define the actual benchmark calls in the body of the functions,
;; that way, stuff can be created before the benchmark starts, based on the
;; arguments
(defn -main [& args]
  (if-not (= 1 (count args))
    (println "usage: lein benchmark benchmark-spec.csv")

    (do
      (with-open [in-file (io/reader (nth args 0))]
        (doall (map run-line (rest (line-seq in-file))))))))
