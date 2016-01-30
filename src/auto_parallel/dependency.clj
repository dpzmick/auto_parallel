(ns auto-parallel.dependency
  (:require [auto-parallel.ast-crawl :refer :all]
            [auto-parallel.util :refer :all]
            [clojure.walk :refer :all]))

;; check for dependencies
(declare dep-in-expr?) ;; defined later

;; most binding forms expand to a let*, so do the macroexpand first
(defn dependency?  [var-name expr] (dep-in-expr? var-name (macroexpand-all expr)))
(defn depend-on-any? [names expr] (any-true? (map #(dependency? % expr) names)))

(defn dep-in-let?
  [bindings forms var-name]
   (if (empty? bindings)
     ;; check in all the sub forms of the let expression
     (any-true? (map #(dependency? var-name % ) forms))

     ;; we still have some bindings to evaluate
     (let
       [first-name  (first  (first bindings))
        first-value (second (first bindings))]

       ;; if we rebind the value, there is no dependency in anything
       ;; inheriting this environment, just need to check that the binding
       ;; does not use the variable in question
       (if (= first-name var-name)
         (dependency? var-name first-value)

         ;; otherwise, the binding may still depend on the name, or the rest of
         ;; the bindings might depend on the name
         (or (dependency? var-name first-value)
             (dep-in-let? (rest bindings) forms var-name))))))

(defn dep-in-normal-expr?  [expr var-name]
  (any-true? (map #(dependency? var-name %) expr)))

(defn dep-in-const? [expr var-name] (= var-name expr))

(defn dep-in-expr? [var-name expr]
  (ast-crawl-expr
    expr
    {:let-cb    dep-in-let?
     :vector-cb dep-in-normal-expr?
     :list-cb   dep-in-normal-expr?
     :const-cb  dep-in-const?}
    var-name))

(defn let-has-deps? [bindings]
  (let
    [pairs          (partition 2 bindings)
     names          (map first pairs)
     exprs          (map second pairs)
     relevant-names (butlast (iterative-list-accum names))]
    (any-true? (map depend-on-any? relevant-names exprs))))
