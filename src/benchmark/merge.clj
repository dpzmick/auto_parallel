(ns benchmark.merge
  (:require [auto-parallel.core :as ap]))

;; helper
(defn merge-seqs
  "Merges two sorted sequences into a single sorted sequence"
  ([left right]
    (merge-seqs (list left right)))
  ([[left right]]
    (loop [l left, r right, result []]
      (let [lhead (first l), rhead (first r)]
        (cond
          (nil? lhead)     (concat result r)
          (nil? rhead)     (concat result l)
          (<= lhead rhead) (recur (rest l) r (conj result lhead))
          true             (recur l (rest r) (conj result rhead)))))))

(defn merge-sort
  [lst]
  (if (= 1 (count lst))
    lst
    (let
      [middle (/ (count lst) 2)]
      (let
        [front  (merge-sort (take middle lst))
         back   (merge-sort (drop middle lst))]
        (merge-seqs front back)))))

(defn merge-sort-parlet1
  [lst]
  (if (= 1 (count lst))
    lst
    (let
      [middle (/ (count lst) 2)]
      (ap/parlet
        [front  (merge-sort (take middle lst))
         back   (merge-sort (drop middle lst))]
        (merge-seqs front back)))))

(defn merge-sort-parlet
  [lst]
  (if (= 1 (count lst))
    lst
    (let
      [middle (/ (count lst) 2)]
      (ap/parlet
        [front  (merge-sort-parlet (take middle lst))
         back   (merge-sort-parlet (drop middle lst))]
        (merge-seqs front back)))))

; (defn merge-sort-parexpr
;   [pool lst]
;   (if (= 1 (count lst))
;     lst
;     (let
;       [middle (/ (count lst) 2)
;        front  (take middle lst)
;        back   (drop middle lst)]
;       (ap/parexpr pool (merge-seqs (merge-sort-parexpr pool front) (merge-sort-parexpr pool back))))))

(defn merge-sort-futures [lst]
  (if (= 1 (count lst))
    lst
    (let
      [middle (/ (count lst) 2)
       front  (future (merge-sort-futures (take middle lst)))
       back   (future (merge-sort-futures (drop middle lst)))]
      (merge-seqs @front @back))))

;; merge
; (def n-merge 100000)
; (def merge-list (doall (take n-merge (repeatedly #(rand-int n-merge)))))
; (defb merge-recur   (merge-sort merge-list))
; (defb merge-parlet1 (merge-sort-parlet1 merge-list))
; (defb merge-parlet  (merge-sort-parlet merge-list))
; (defb merge-futures (merge-sort-futures merge-list))

; (def merge-benchmarks [
;                        merge-parlet
;                        merge-recur
;                        ; merge-parlet1
;                        ;merge-futures ;; this isn't useful, runs out of memory
;                        ])
