(ns benchmark.search
  (:require [criterium.core :as cr])
  (:require [com.dpzmick.util :refer :all])
  (:require [com.dpzmick.parallel-macros.defparfun :refer [defparfun]]))

(defparfun searchpar [value lst] false
  (do (println "called") (Thread/sleep 5)
  (cond
    (> 1 (count lst)) false
    (= 1 (count lst)) (= value (first lst))
    :else             (let [mid (/ (count lst) 2)
                            a   (take mid lst)
                            b   (drop mid lst)]
                        (if (= value (nth lst mid))
                          true
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

(defn- make-list [n] (vec (take n (repeatedly #(rand-int n)))))

(defn search-serial [n]
  (let [search-list (make-list n)]
    (cr/bench (searchserial 1 search-list))))
