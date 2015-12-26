(ns auto-parallel.ast-crawl-test
  (:use auto-parallel.ast-crawl)
  (:use clojure.test))

;; test that the callbacks get called with the right value and the right args
(deftest test-const-cb
  (let
    [cb (fn [e args] (is (and (= e 'a) (= args 'meow))))]
    (ast-crawl-expr 'a {:const-cb cb} 'meow)))

(deftest test-let-cb
  (let
    [cb (fn [e args] (is (and (= e '(let* silly)) (= args 'meow))))]
    (ast-crawl-expr '(let* silly) {:let-cb cb} 'meow)))

(deftest test-vector-cb
  (let
    [cb (fn [e args] (is (and (= e '[a b c]) (= args 'meow))))]
    (ast-crawl-expr '[a b c] {:vector-cb cb} 'meow)))

(deftest test-sequential-cb
  (let
    [cb (fn [e args] (is (and (= e '(foo bar)) (= args 'meow))))]
    (ast-crawl-expr '(foo bar) {:sequential-cb cb} 'meow)))
