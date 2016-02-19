(ns com.dpzmick.ast-manip.dependency-test
  (:require [com.dpzmick.ast-manip.dependency :refer :all]
            [clojure.test :refer :all]))

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

(deftest dep-in-map
  (is (dependency? 'a '{:a a}))
  (is (dependency? 'a '{:a b :b a}))
  (is (dependency? 'a '{a 100}))
  (is (not (dependency? 'a '{:a (let [a 10] a)}))))
