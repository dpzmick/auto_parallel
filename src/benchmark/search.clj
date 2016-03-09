(ns benchmark.search
  (:require [com.dpzmick.util :refer :all])
  (:require [clojure.string :as str])
  (:require [criterium.core :as cr])
  (:require [com.dpzmick.util :refer :all])
  (:require [com.dpzmick.parallel-macros.defparfun :refer [defparfun]]))

(def found (atom false))
(defparfun searchpar [value lst] (< 10000 (count lst))
  (if @found
    false

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

                            ;; we can't short circuit here without serializing
                            ;; TODO I could maybe detect things like this and warn
                            ;; the user
                            ;; (or (searchpar value a) (searchpar value b)))))))

                            ;; could use a parlet here, but using defparfun for
                            ;; grain
                            (let
                              [force-l (searchpar value a)
                               force-r (searchpar value b)]
                              (or force-l force-r)))))))

(defn searchserial [value lst]
  (cond
    (> 1 (count lst)) false
    (= 1 (count lst)) (= value (first lst))
    :else             (let [mid (/ (count lst) 2)
                            a   (take mid lst)
                            b   (drop mid lst)]
                        (if (= value (nth lst mid))
                          true
                          (or (searchserial value a) (searchserial value b))))))

(defn- list-from-file [filename]
  (doall (map read-string (str/split (slurp filename) #"\n"))))

(defn- make-list [n] (vec (take n (repeatedly #(rand-int n)))))

(defn search-serial [filename]
  (let [search-list (list-from-file filename)]
    (cr/bench (searchserial 1 search-list))))

(defn search-parfun [filename]
  (swap! found (fn [n] false))
  (let [search-list (list-from-file filename)]
    (cr/bench (searchpar 1 search-list))))
