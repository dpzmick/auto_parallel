(ns auto-parallel.ident-calls-test
  (:require [auto-parallel.ident-calls :refer :all]
            [clojure.test :refer :all]))

(deftest vector-tests
  (is (= '(foo meow) (identify-inner-call-of 'foo '[(foo meow) bar car])))
  (is (empty? (identify-inner-call-of 'foo '[foo bar car]))))

(deftest quoted-list
  (is (empty? (identify-inner-call-of 'foo '(quote foo bar)))))

(deftest simple-call
  (is (= '(foo meow) (identify-inner-call-of 'foo '(foo meow))))
  (is (= '(foo meow) (identify-inner-call-of 'foo '(foo meow)))))

(deftest let-tests
  (is
    (= '(foo meow)
       (identify-inner-call-of 'foo '(let* [a b c d] (foo meow)))))
  (is
    (= '(foo meow)
       (identify-inner-call-of 'foo '(let* [a (foo meow) c d] (foo car)))))

  (is
    (= '(foo meow)
       (identify-inner-call-of 'foo '(let* [foo (foo meow) c d] (foo car)))))

  (is
    (empty? (identify-inner-call-of 'foo '(let* [foo b c d] (foo b)))))

  (is
    (empty? (identify-inner-call-of 'foo '(let* [foo b c (foo meow)] (foo b)))))

  (is
    (empty? (identify-inner-call-of 'foo '(let* [foo b c (foo meow)] (foo b)))))

  (is
    (empty? (identify-inner-call-of 'foo '(let* [a b c d] (a b))))))
