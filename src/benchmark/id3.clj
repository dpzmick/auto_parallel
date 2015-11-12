(ns benchmark.id3)

(defn rand-bool [] (case (rand-int 2)
                     0   true
                     1   false))

;; id3 for datasets where all attributes are binary attributes
;; example of a datum {:a true :b false :c true}
(defn log2 [n] (/ (Math/log n) (Math/log 2)))

;; this is sort of useless but gives us nice naming I guess
(defn has? [attr datum] (attr datum))

(defn entropy [dataset attr]
  (let
    [with     (filter #(has? attr %) dataset)
     without  (filter #(not (has? attr %)) dataset)
     cwith    (count with)
     cwithout (count without)
     cdata    (count dataset)]
    (+
     (- (* (/ cwith cdata) (log2 (/ cwith cdata))))
     (- (* (/ cwithout cdata) (log2 (/ cwithout cdata)))))))

;; why does this use more than one cpu core?
(defn id3
  "builds a tree using id3"
  [dataset attrs target]
  (let
    [sattrs     (sort-by #(entropy dataset %) attrs)]
    (if
      (or (= (first sattrs) target) (empty? attrs))
      []
      (let [split-attr (first sattrs)
            left       (filter #(not (has? split-attr %)) dataset)
            right      (filter #(has? split-attr %) dataset)]
        (cons split-attr [(id3 left  (rest sattrs) target)
                          (id3 right (rest sattrs) target)])))))

(defn make-random-data
  [n attrs]
  (let [rand-datum (fn [] (reduce #(assoc %1 %2 (rand-bool)) {} attrs))]
    (take n (repeatedly #(rand-datum)))))

(defn make-attrs [m] (take m (map #(keyword (str %)) (range))))
