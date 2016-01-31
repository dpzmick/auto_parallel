(ns auto-parallel.core
  (:require [auto-parallel.dependency :refer :all]
            [auto-parallel.ident-calls :refer :all]
            [auto-parallel.util :refer :all]
            [auto-parallel.replace :refer :all]))

(def dependency-error
  "this let form cannot be parallel. There are dependencies in the bindings")

(defmacro parlet
  [bindings & forms]
  (if (let-has-deps? bindings)
    (throw (Exception. dependency-error))

    (let
      [pairs    (partition 2 bindings)
       names    (map first pairs)
       vals     (map second pairs)
       tasks    (vec (map (fn [v] `(p/fork (p/new-task (fn [] ~v)))) vals))
       new-vals (map (fn [n] `(p/join ~n)) names)]

      ;; each val becomes a fork join task, each reference to the value becomes
      ;; (join task)

      ;; use pattern matching to express this
      `(let
         ~(make-bindings names tasks)
         ~@(map #(replace-many names new-vals %) forms)))))

;; parexpr
;; make it easier to play with syntax tree
(defn args [expr] (rest expr))
(defn fun  [expr] (first expr))
(defn const? [expr] (not (seq? expr)))
(defn all-args-const?
  [expr]
  (every? true? (map const? (args expr))))

(defn prune
  "
  finds 'leaves' in the syntax tree. Everything in :names can be computed in
  parallel
  "
  [expr]
  (cond
    (const? expr)
    {:expr expr, :names {}}

    (all-args-const? expr)
    (let
      [n (gensym "expr")]
      {:expr n, :names {n expr}})

    :else
    (let
      [subresults (map prune (args expr))
       subexprs   (map :expr subresults)
       subnames   (map :names subresults)
       mynames    (apply merge subnames)]
      {:expr (cons (fun expr) subexprs), :names mynames})))

(defn make-nested-lets
  "
  makes nested parlets such that parallelism is maximized
  "
  [expr]
  (if (const? expr)
    expr
    (let
      [{e :expr n :names} (prune expr)
       bindings           (vec (apply concat (into (list) n)))]
      `(parlet ~bindings ~(make-nested-lets e)))))

(defmacro parexpr [expr] (make-nested-lets expr))

;; first step, move each function call into a let binding in the immediate
;; vicinity of the function call

; (defmacro defparfun [method-name formals body]
;   ;; replace all recursive calls
;   (let [rep-body (replace-all-calls method-name body)]
;     `(defn ~method-name ~formals ~rep-body)))

; (defn fib [n]
;   (if (<= 1 n)
;     1
;     (+ (fib (- n 1)) (fib (- n 2)))))

; ;; can't reorder in this way
; (defn fib [n]
;   (let
;     [l (fib (- n 1))
;      r (fib (0 n 2))]
;   (if (<= 1 n)
;     1
;     (+ l r))))

; ;; but this isn't useful
; (defn fib [n]
;   (if (<= 1 n)
;     1
;     (+
;      (let
;        [l (fib (- n 1))]
;       l)
;     (let
;       [r (fib (- n 2))]
;       r))))

; ;; I really need it to do this:
; (defn fib [n]
;   (if (<= 1 n)
;     1
;     ;; how do I know to put the fib calls here?
;     ;; how do I know they can both go here?
;     ;; what phases can I use? Should I create a let-reordering phase that can
;     ;; compress lets with no flow control
;     (let [l (fib (- n 1))
;           r (fib (- n 2))]
;       (+ l r))))
