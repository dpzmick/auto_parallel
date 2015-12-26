(ns auto-parallel.replace
  (:use [clojure.walk])
  (:use [auto-parallel.ast-crawl]))

;; replace-all
(def replace-in-expr) ;; defined later

;; most binding forms expand to a let*, so do the macroexpand first
(defn replace-all    [e replacement expr] (replace-in-expr e replacement (macroexpand-all expr)))
(defn replace-many   [es reps expr]
  (if (empty? es)
    expr
    (recur
      (rest es)
      (rest reps)
      (replace-all (first es) (first reps) expr))))

(defn replace-in-let
  ([expr {e :to-replace, replacement :replacement}]
   (replace-in-let e replacement (partition 2 (second expr)) (rest (rest expr))))

  ([e replacement bindings forms]
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
            ~(replace-in-let e replacement (rest bindings) forms)))))))

;; is there a "map preserving input type" anywhere?
(defn replace-in-seq [expr {e :to-replace, replacement :replacement}]
  (map #(replace-all e replacement %) expr))

(defn replace-in-vector [expr args]
  (vec (replace-in-seq expr args)))

(defn replace-in-const [expr {e :to-replace, replacement :replacement}]
  (if  (= e expr) replacement expr))

(defn replace-in-expr [e replacement expr]
  "replace all occurrences of e with replacement in expr"
  (ast-crawl-expr
    expr
    ;; callbacks
    {:let-cb        replace-in-let
     :vector-cb     replace-in-vector
     :sequential-cb replace-in-seq
     :const-cb      replace-in-const}

    ;; args
    {:to-replace  e
     :replacement replacement}))
