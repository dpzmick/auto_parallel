(ns benchmark.fib
  (:require [criterium.core :as cr])
  (:require [com.dpzmick.parallel-macros.parexpr :refer [parexpr]])
  (:require [com.dpzmick.parallel-macros.parlet :refer [parlet]])
  (:require [com.dpzmick.parallel-macros.defparfun :refer [defparfun]]))

(defn fib ^Long [^Long n]
  (if (or (= 0 n) (= 1 n))
    1
    (+
     (fib (- n 1))
     (fib (- n 2)))))

(defn fibparexpr
  [n]
  (if (or (= 0 n) (= 1 n))
    1
    (parexpr (+ (fibparexpr (- n 1))
                (fibparexpr (- n 2))))))

(defn fibparlet [^Long n]
  (if (or (= 0 n) (= 1 n))
    1
    (parlet
      [m1 (fibparlet (- n 1))
       m2 (fibparlet (- n 2))]
      (+ m1 m2))))

;; TODO where did my type annotation go :O
(defparfun fibparfun [^Long n] (< n 35)
  (if (or (= 0 n) (= 1 n))
    1
    (+
     (fibparfun (- n 1))
     (fibparfun (- n 2)))))

;; we quickly run out of threads with this
(defn fibfuture [n]
  (if (or (= 0 n) (= 1 n))
    1
    (let [a (future (fibfuture (- n 1)))
          b (future (fibfuture (- n 2)))]
      (+ @a @b))))

;; define a handful of benchmark functions
(defn fib-parfun [nstr]
  (cr/bench (fibparfun (read-string nstr))))

(defn fib-parexpr [nstr]
  (cr/bench (fibparexpr (read-string nstr))))

(defn fib-parlet [nstr]
  (cr/bench (fibparlet (read-string nstr))))

(defn fib-serial [nstr]
  (cr/bench (fib (read-string nstr))))

(defn fib-future [nstr]
  (cr/bench (fibfuture (read-string nstr))))
