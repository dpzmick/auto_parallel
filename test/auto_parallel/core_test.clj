(ns auto-parallel.core-test
  (:use auto-parallel.core)
  (:use clojure.test))

;; dependency?
(deftest simple-no-dep
  (is (not (dependency? 'cat '(feed dog)))))

(deftest simple-dep
  (is (dependency? 'cat '(feed cat))))

(deftest array-dep
  (is (dependency? 'cat '(feed [cat dog]))))

(deftest no-dep-because-of-let
  (is (not (dependency? 'a '(let [a b] (a))))))

(deftest dep-in-let
  (is (dependency? 'a '(let [c a] c))))

(deftest dep-in-let2
  (is (dependency? 'a '(let [c d] a))))

(deftest dep-in-let3
  (is (dependency? 'a '(let [c d b a] a))))

(deftest dep-in-let4
  (is (dependency? 'a '(let [a a] a))))
