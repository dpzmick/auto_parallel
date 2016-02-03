(ns com.dpzmick.ast-manip.replace
  (:require [com.dpzmick.ast-manip.ast-crawl :refer :all]
            [clojure.walk :refer :all]))

(declare replace-in-expr) ;; defined later

;; most binding forms expand to a let*, so do the macroexpand first
(defn replace-all
  "replace all occurrences of e with replacement in expr"
  [e replacement expr]
  (replace-in-expr
    (macroexpand-all expr)
    {:to-replace e
     :replacement replacement}))

(defn replace-many
  "
  for e, rep in zip(es, reps):
    replace-all e rep
  "
  [es reps expr]
  (if (empty? es)
    expr
    (recur
      (rest es)
      (rest reps)
      (replace-all (first es) (first reps) expr))))

;; ast functions
(defn replace-in-let
  [bindings forms {e :to-replace, replacement :replacement :as args}]
  (let
    [first-name  (first  (first bindings))
     first-value (second (first bindings))
     replaced-v  (replace-all e replacement first-value)]

    ;; if we rebind the value, there is no dependency in anything
    ;; inheriting this environment, just need to perform replacement in the
    ;; binding, then emit the rest of the expression as is
    (if (= first-name e)
      `(let
         [~first-name ~replaced-v]
         (let
           ~(vec (flatten (rest bindings)))
           ~@forms))

      ;; otherwise, the binding may still depend on the name, some other
      ;; binding might, or the body might
      (if (= (count bindings) 1)
        ;; last one
        `(let
           [~first-name ~replaced-v]
           ~@(map #(replace-all e replacement %) forms))

        ;; still some to go
        `(let
           [~first-name ~replaced-v]
           ~(replace-in-let (rest bindings) forms args))))))

(defn replace-in-list [expr {e :to-replace, replacement :replacement}]
  (if (= e replacement) 'buhh
    (map #(replace-all e replacement %) expr)))

(defn replace-in-vector [expr args]
  (vec (replace-in-list expr args)))

(defn replace-in-const [expr {e :to-replace, replacement :replacement}]
  (if (= e expr) replacement expr))

(defn replace-in-expr [expr {e :to-replace replacement :replacement :as args}]
  ;; if we are trying to replace a whole expression, we may have found it
  (ast-crawl-expr
    expr
    ;; callbacks
    {:let-cb    replace-in-let
     :vector-cb replace-in-vector
     :list-cb   replace-in-list
     :const-cb  replace-in-const}
    args))
