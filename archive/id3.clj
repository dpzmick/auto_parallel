;; id3 for datasets where all attributes are binary attributes
;; example of a datum {:a true :b false :c true}

(ns benchmark.id3
  (:require [auto-parallel.core :as ap]))

(use 'clojure.tools.trace)

(defn rand-bool [] (case (rand-int 2)
                     0   true
                     1   false))

(defn log2 [n] (/ (Math/log n) (Math/log 2)))

;; this is sort of useless but gives us nice naming I guess
(defn has? [attr datum] (attr datum))

(defn entropy [dataset attr] {:post [(not (Double/isNaN %))]}
  (let
    [with     (filter #(has? attr %) dataset)
     without  (filter #(not (has? attr %)) dataset)
     cwith    (count with)
     cwithout (count without)
     cdata    (count dataset)]
    (if (or (= cwith cdata) (= cwithout cdata))
      0.0
      (+
       (- (* (/ cwith cdata) (log2 (/ cwith cdata))))
       (- (* (/ cwithout cdata) (log2 (/ cwithout cdata))))))))

(defn id3
  "builds a tree using id3"
  [dataset attrs target]
  (if (or (empty? dataset) (empty? attrs))
    []
    (let
      [sattrs (sort-by #(entropy dataset %) attrs)]
      (if (= (first sattrs) target)
        []
        (let [split-attr (first sattrs)
              left       (filter #(not (has? split-attr %)) dataset)
              right      (filter #(has? split-attr %) dataset)]
          (cons split-attr [(id3 left  (rest sattrs) target)
                            (id3 right (rest sattrs) target)]))))))

(defn make-random-data
  [n attrs]
  (let [rand-datum (fn [] (reduce #(assoc %1 %2 (rand-bool)) {} attrs))]
    (take n (repeatedly #(rand-datum)))))

(defn make-attrs [m] (take m (map #(keyword (str %)) (range))))

(defn doid3 [n m]
  (let
    [attrs (make-attrs m)
     data  (make-random-data n attrs)]
    (id3 data (rest attrs) (first attrs))))
