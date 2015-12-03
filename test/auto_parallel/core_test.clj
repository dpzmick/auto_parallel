(ns auto-parallel.core-test
  (:use clojure.walk)
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

;; test replacement
;; do the macroexpand to give everything the same names
(defmacro defreptest [n e r input output]
  `(deftest ~n
     (is (=
          (macroexpand-all (quote ~output))
          (macroexpand-all (replace-all (quote ~e) (quote ~r) (quote ~input)))))))

(defreptest replace-single a b a b)

(defreptest replace-argument a b (+ a b) (+ b b))

(defreptest replace-let-in-body a b (let [c :d] a) (let [c :d] b))

;; multiple bindings get expanded into multiple lets
(defreptest replace-let-in-body2 a b
  (let [c :d e :f] a)
  (let [c :d] (let [e :f] b)))

;; we end up with extra because of property that makes code easier
(defreptest replace-early-stop a b
  (let [a :b] a)
  (let [a :b] (let [] a)))

(defreptest replace-early-stop-multiple a b
  (let [a :b c :d e :f] a)
  (let [a :b] (let [c :d e :f] a)))

(defreptest replace-nested-lets a b
  (let [c :d] (let [e :f] a))
  (let [c :d] (let [e :f] b)))

(defreptest preserve-vector a b
  [a b c]
  [b b c])
