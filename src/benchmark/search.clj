(ns benchmark.search
  (:require [com.dpzmick.util :refer :all])
  (:require [clojure.string :as str])
  (:require [criterium.core :as cr])
  (:require [com.dpzmick.util :refer :all])
  (:require [com.dpzmick.parallel-macros.defparfun :refer [defparfun]]))

(declare searchpar)

(def found (atom false))
(defn parwrap [value lst]
  (swap! found (fn [n] false))
  (searchpar value lst))

(defparfun searchpar [value lst] (< 10000 (count lst))
  (if @found
    true

    (cond
      (> 1 (count lst)) false
      (= 1 (count lst)) (= value (first lst))
      :else             (let [mid (/ (count lst) 2)
                              a   (doall (take mid lst))
                              b   (doall (drop mid lst))]
                          (if (= value (nth lst mid))
                            (do
                              (swap! found (fn [n] true))
                              true)

                            (or (searchpar value a) (searchpar value b)))))))

(declare searchserial)

(def foundser (atom false))
(defn serwrap [value lst]
  (swap! foundser (fn [n] false))
  (searchserial value lst))

(defn searchserial [value lst]
  (if @foundser
    true

    (cond
      (> 1 (count lst)) false
      (= 1 (count lst)) (= value (first lst))
      :else             (let [mid (/ (count lst) 2)
                              a   (doall (take mid lst))
                              b   (doall (drop mid lst))]
                          (if (= value (nth lst mid))
                            (do
                              (swap! foundser (fn [n] true))
                              true)

                            (or (searchserial value a) (searchserial value b)))))))


(defn- list-from-file [filename]
  (doall (map read-string (str/split (slurp filename) #"\n"))))

(defn- make-list [n] (vec (take n (repeatedly #(rand-int n)))))

(defn search-serial [filename]
  (let [search-list (list-from-file filename)]
    (cr/bench (serwrap 1 search-list))))

(defn search-parfun [filename]
  (let [search-list (list-from-file filename)]
    (cr/bench (parwrap 1 search-list))))
