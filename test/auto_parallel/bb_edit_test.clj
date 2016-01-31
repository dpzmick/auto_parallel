(ns auto-parallel.bb-edit-test
  (:require [auto-parallel.bb-edit :refer :all]
            [auto-parallel.expand-lets :refer :all]
            [clojure.pprint :refer :all]
            [clojure.test :refer :all]))

;; the only way to really test this is to define some stuff an make sure that
;; everything evaluates correctly
;; that's pretty questionable, but since random variable names are being
;; introduced I think that's probably the best way to test

;; a function to use
(defn f
  ([x y]
   (if y
     (throw (Exception. "should not have called this function"))
     x))
  ([x] (f x false)))

(defn meval [expr] ((eval (list 'fn ['f] expr)) f))

(deftest simple-tests
  ;; immediate call
  (let
    [in (list 'f 10 false)
     out (move-calls-to-header in 'f)]

    (println "1:" (meval out))
    (is (= 10 (meval out))))

  ;; simple if statement
  (let
    [in '(if (= 0 0) (f 10) (f 10 true))
     out (move-calls-to-header in 'f)]
    (println "2:" (meval out))
    (is (= 10 (meval out))))

  ;; try a vector
  (let
    [in '(if (= 0 0)
           [(f 10) (f 11) (f 12)]
           (f 10 true))
     out (move-calls-to-header in 'f)]
    (println "3:" (meval out))
    (is (= [10 11 12] (meval out))))

  ;; make sure it doesn't break simple code
  (let
    [in '(let [a 10 b 10] (+ a b))
     out (move-calls-to-header (expand-lets in) 'f)]
    (println "4:" (meval out))
    (is (= 20 (meval out)))))

 (deftest let-tests
   ;; check that dep order doesn't get ruined
   (let
     [in '(let [a (f 10) b (f a)] b)
      out (move-calls-to-header (expand-lets in) 'f)]
     (println "5:" (meval out))
     (is (= 10 (meval out))))

   ;; do something wild
   (let
     [in '(let [a (f 10) b (f a)] (f (f b)))
      out (move-calls-to-header (expand-lets in) 'f)]

     (println "6:" (meval out))
     (is (= 10 (meval out))))

   (let
     [in '(let
            [a (f 10)
             b (f a)]
            (f
              (if (= a 5)
                (f 10 true)
                (f b))))

      out (move-calls-to-header (expand-lets in) 'f)]

     (println "7:" (meval out))
     (pprint out)
     (is (= 10 (meval out))))
  )
