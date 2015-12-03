(ns auto-parallel.replace
  (use [clojure.walk]))

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
  ([e replacement expr]
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
(defn replace-in-seq-expr [e replacement expr]
  (map #(replace-all e replacement %) expr))

(defn replace-in-vector-expr [e replacement expr]
  (vec (replace-in-seq-expr e replacement expr)))

(defn replace-in-const [e replacement expr] (if  (= e expr) replacement expr))

(defn replace-in-expr
  [e replacement expr]
  (if (sequential? expr)

    ;; we have a function call or some other form (like a vector or list
    ;; literal)
    (cond
      (= 'let* (first expr)) (replace-in-let e replacement expr)
      (vector? expr)         (replace-in-vector-expr e replacement expr)
      (seq? expr)            (replace-in-seq-expr e replacement expr)
      :else                  (throw (Exception. "this form is not supported")))

    ;; the expression is a single term
    (replace-in-const e replacement expr)))
