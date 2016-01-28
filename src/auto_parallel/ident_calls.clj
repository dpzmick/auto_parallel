(ns auto-parallel.ident-calls
  (:use [auto-parallel.ast-crawl]))

(def identify-inner-call-of)

;; won't ever happen in a constant
(defn identify-inner-call-const [expr _] nil)

(defn identify-inner-call-vector [expr fname]
  (first (map #(identify-inner-call-of fname %) expr)))

;; have to check if this list is a function call
(defn identify-inner-call-list [expr fname]
  (let
    [children (remove nil? (map #(identify-inner-call-of fname %) (rest expr)))]

    (if (= (first expr) fname)
      (if (empty? children)
        expr
        (first children))
      (first children))))

(defn identify-inner-call-let [bindings forms fname]
  (if (empty? bindings)
    ;; if no bindings left, just explore the forms
    (first (remove nil? (map #(identify-inner-call-of fname %) forms)))

    ;; if there are bindings left, look in those
    (let
      [first-name  (first  (first bindings))
       first-value (second (first bindings))
       first-child (identify-inner-call-of fname first-value)]
      (if (not (nil? first-child))
        ;; we can return the first child
        first-child

        ;; we need to keep looking
        (if (= first-name fname)
          ;; if the names are the same, the function has been rebound from this
          ;; point on
          nil

          ;; otherwise, keep looking
          (identify-inner-call-let (rest bindings) forms fname))))))

(defn identify-inner-call-of
  "
  finds all the inner most call of the function fname in the expression expr
  "
  [fname expr]
  (ast-crawl-expr
    expr
    {:let-cb        identify-inner-call-let
     :vector-cb     identify-inner-call-vector
     :list-cb       identify-inner-call-list
     :const-cb      identify-inner-call-const}
    fname))
