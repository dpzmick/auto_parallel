(ns com.dpzmick.ast-manip.simplify-ast
  (:require [com.dpzmick.ast-manip.ast-crawl :refer :all]
            [com.dpzmick.ast-manip.replace :refer :all]
            [com.dpzmick.util :refer :all]
            [clojure.walk :refer :all]))

(defn- log [& args] (if false (apply println args)))

(declare crawl)

;; use ast-crawl to avoid any duplication of logic
(defn- const? [expr]
  (ast-crawl-expr
    (macroexpand-all expr)
    {
     :let-cb    (fn [_ _ _] false)
     :vector-cb (fn [_ _]   false)
     :list-cb   (fn [_ _]   false)
     :const-cb  (fn [_ _]   true)
     }))

(defn- crawl-let [bindings forms [fname task-name :as args]]
  (log "crawl-simple-let" bindings forms fname task-name)
  (if (empty? bindings)
    ;; no reason to keep this let around
    ;; emit a do just in case (might need to run this more than once..
    `(do ~@(map crawl forms))

    (if (and
          (= 1 (count bindings))
          (const? (second (first bindings)))) ;; the expresion in the single binding

      ;; do the rename
      (replace-all
        (first  (first bindings))
        (second (first bindings))
        `(do ~@(map crawl forms)))

      ;; otherwise behave normally
      `(let
         ~(make-bindings (map first bindings) (map second bindings))
         ~@(map crawl forms)))))

;; everything is the identify other than let
(defn- crawl-vector [expr args]
  (log "crawl-simple-vector" expr args)
  (mapv crawl expr))

(defn- crawl-list [expr args]
  (log "crawl-simple-list" expr args)
  (if (and
        (= 'do (first expr))
        (= (count expr) 2))

    ;; if we have a do with only one other member
    (crawl (first (rest expr)))

    (map crawl expr)))

(defn- crawl-const  [expr _]
  (log "crawl-simple-const" expr)
  expr)

(defn- crawl [expr]
  (ast-crawl-expr
    (macroexpand-all expr)
    {
     :let-cb crawl-let
     :vector-cb crawl-vector
     :list-cb crawl-list
     :const-cb crawl-const
     }))

(defn simplify-ast
  "
  applies some light simplification to the syntax tree
  (useful in trying to stave off stack overflow)

  This also applies an important modification to let forms. All lets of the form
  (let [a b] ...) (lets which only perform a renaming) are removed. The original
  name is kept, and all occurances of the new name are replaced by the original
  name. So, in the example, the name 'a would be removed, and all occurances would
  be replaces with 'b
  "
  [expr]
  (log "simplify-ast" expr)
  (crawl expr))
