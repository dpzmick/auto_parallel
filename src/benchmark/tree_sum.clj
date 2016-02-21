(ns benchmark.tree-sum
  (:require [criterium.core :as cr])
  (:require [com.dpzmick.util :refer :all])
  (:require [com.dpzmick.parallel-macros.defparfun :refer [defparfun]])
  (:import (java.util.concurrent RecursiveTask)))

;; TODO figure out why sticking defparfun on this does weird things
(defn make-tree [d]
  (do
  (if (= d 0)
    (doall (list d nil nil))
    (doall (list d (make-tree (- d 1)) (make-tree (- d 1)))))))

(defn- val [t]  (first t))
(defn- left [t] (second t))
(defn- right [t] (nth t 2))

(defn sum [subtree]
  (if (and (nil? (left subtree)) (nil? (right subtree)))
    (val subtree)
    (+
     (val subtree)
     (sum (left subtree))
     (sum (right subtree)))))

(defparfun sump [subtree depth depth-num] (> depth depth-num)
  (if (and (nil? (left subtree)) (nil? (right subtree)))
    (val subtree)
    (+
     (val subtree)
     (sump (left subtree) (inc depth) depth-num)
     (sump (right subtree) (inc depth) depth-num))))

(defn sum-serial [n]
  (let [tree (make-tree (read-string n))]
    (println "input constructed, here we go")
    (cr/bench (sum tree))))

;; use a granularity relative to the number of threads that will be used
(defn- depth-num [threads]
  (Math/round (/ (Math/log threads) (Math/log 2))))

(defn sum-parfun [n threads]
  (let [tree (make-tree (read-string n))
        dn   (depth-num (read-string threads))]
    (println "input constructed, here we go")
    (cr/bench (sump tree 0 dn))))
