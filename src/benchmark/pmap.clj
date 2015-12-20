(ns benchmark.pmap
  (:require [auto-parallel.fork-join-par :as ap]))

(defn prime? [n]
  (every? #(pos? (rem n %)) (range 2 (Math/sqrt (inc n)))))

(defn nth-prime [n]
  (loop [i 0 d 1]
    (if (prime? d)
      (if (= i n)
        d
        (recur (+ 1 i) (+ 1 d)))
      (recur i (+ 1 d)))))

;; determine which pmap performs better for a long running function
; (defn fj-pmap [pool lst]
;   (doall (ap/pmap pool nth-prime lst)))

; (defn builtin-pmap [lst]
;   (doall (pmap nth-prime lst)))

;; pmap test
; (def n-pmap 200)
; (def list-pmap (take n-pmap (repeatedly #(rand-int 1000))))
; (defb fj-pmap-bench (fj-pmap pool list-pmap))
; (defb builtin-pmap-bench (builtin-pmap list-pmap))

; (def pmap-bechmarks [
;                      fj-pmap-bench
;                      builtin-pmap-bench
;                      ])
