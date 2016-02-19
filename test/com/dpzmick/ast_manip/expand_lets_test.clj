(ns com.dpzmick.ast-manip.expand-lets-test
  (:require [com.dpzmick.ast-manip.expand-lets :refer :all]
            [clojure.walk :refer :all]
            [clojure.test :refer :all]))

;; these are sort of messy
(deftest expand-simple
  (let
    [in '(let [a 10 b 10] a)
     out '(let [a 10] (let [b 10] (do a)))]

    (is
      (= (macroexpand-all out)
         (macroexpand-all (expand-lets in))))))

;; wow this sucks
;; these dos are annoying
(deftest expand-nested
  (let
    [in '(let [a 10 b 10] (let [c 10] c))
     out '(let [a 10] (let [b 10] (do (let [c 10] (do c)))))]

    (is
      (= (macroexpand-all out)
         (macroexpand-all (expand-lets in))))))

;; do this one with an eval lol
(deftest expand-vector-destructure
  (let
    [in '(let [[a b] [10 11]] (+ a b))]
    (is
      (= 21 (eval (expand-lets in))))))

(deftest expand-map-lets
  (let
    [in '(:a {:a 10})]
    (is (= 10 (eval (expand-lets in)))))

  (let
    [in '(get {(let [a 10 b 10] (+ a b)) 10} 20)]
    (is (= 10 (eval (expand-lets in))))
    (println (expand-lets in)))

  (let
    [in '(:a {:a (let [a 10 b 10] (+ a b))})]
    (is (= 20 (eval (expand-lets in))))
    (println (expand-lets in))))
