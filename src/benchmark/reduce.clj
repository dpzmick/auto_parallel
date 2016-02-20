(ns benchmark.reduce
  (:require [criterium.core :as cr])
  (:require [com.dpzmick.util :refer :all])
  (:require [com.dpzmick.parallel-macros.defparfun :refer [defparfun]]))

(defn- make-tree [d]
  (if (= d 0)
    (list (rand-int 100) nil nil)
    (list (rand-int 100) (make-tree (- d 1)) (make-tree (- d 1)))))

(defn- val [t]  (first t))
(defn- left [t] (second t))
(defn- right [t] (nth t 2))

;; tree is global
(defn sum [subtree]
  (if (and (nil? (left subtree)) (nil? (right subtree)))
    (val subtree)
    (+
     (sum (left subtree))
     (sum (right subtree)))))

(defparfun sump [subtree depth] (> depth 2)
  (if (and (nil? (left subtree)) (nil? (right subtree)))
    (val subtree)
    (+
     (sump (left subtree) (inc depth))
     (sump (right subtree) (inc depth)))))
