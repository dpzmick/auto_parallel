(ns auto-parallel.ast-crawl-test
  (:require [auto-parallel.ast-crawl :refer :all]
            [clojure.test :refer :all]))

;; test that the callbacks get called with the right value and the right args
(deftest test-const-cb
  (let
    [cb (fn [e args] (is (and (= e 'a) (= args 'meow))))]
    (ast-crawl-expr 'a {:const-cb cb} 'meow)))

(deftest test-let-cb
  (let
    [cb (fn [bindings forms args]
          (is (= (first (first bindings)) 'silly1))
          (is (= (second (first bindings)) 'silly2))
          (is (= forms '(silly3)))
          (is (= args 'meow)))]

    (ast-crawl-expr '(let* [silly1 silly2] silly3) {:let-cb cb} 'meow)))

(deftest test-vector-cb
  (let
    [cb (fn [e args] (is (and (= e '[a b c]) (= args 'meow))))]
    (ast-crawl-expr '[a b c] {:vector-cb cb} 'meow)))

(deftest test-sequential-cb
  (let
    [cb (fn [e args] (is (and (= e '(foo bar)) (= args 'meow))))]
    (ast-crawl-expr '(foo bar) {:list-cb cb} 'meow)))
