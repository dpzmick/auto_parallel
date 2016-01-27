(ns auto-parallel.dependency
  (:use [auto-parallel.ast-crawl])
  (:use [clojure.walk])
  (:use [auto-parallel.util]))

;; check for dependencies
(def dep-in-expr?) ;; defined later

;; most binding forms expand to a let*, so do the macroexpand first
(defn dependency?  [var-name expr] (dep-in-expr? var-name (macroexpand-all expr)))
(defn depend-on-any? [names expr] (any-true? (map #(dependency? % expr) names)))

(defn dep-in-let?
  [bindings forms {var-name :var-name :as args}]
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
             (dep-in-let? (rest bindings) forms args))))))

(defn dep-in-normal-expr?  [expr {var-name :var-name}]
  (any-true? (map #(dependency? var-name %) expr)))

(defn dep-in-const? [expr {var-name :var-name}] (= var-name expr))

(defn dep-in-expr? [var-name expr]
  (ast-crawl-expr
    expr
    {:let-cb    dep-in-let?
     :vector-cb dep-in-normal-expr?
     :list-cb   dep-in-normal-expr?
     :const-cb  dep-in-const?}
    {:var-name var-name}))

(defn let-has-deps? [bindings]
  (let
    [pairs          (partition 2 bindings)
     names          (map first pairs)
     exprs          (map second pairs)
     relevant-names (butlast (iterative-list-accum names))]
    (any-true? (map depend-on-any? relevant-names exprs))))
