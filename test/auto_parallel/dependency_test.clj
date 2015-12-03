(ns auto-parallel.dependency-test
  (:use auto-parallel.dependency)
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
  (is (dependency? 'a '(let [c a] c)))
  (is (dependency? 'a '(let [c d] a)))
  (is (dependency? 'a '(let [c d b a] a)))
  (is (dependency? 'a '(let [a a] a))))
