(ns auto-parallel.simplify-ast
  (:require [auto-parallel.replace :refer :all]
            [auto-parallel.ast-crawl :refer :all]
            [auto-parallel.fork-join-par :as fj]
            [clojure.pprint :refer :all]
            [auto-parallel.util :refer :all]
            [clojure.walk :refer :all]))

(defn- log [& args] (if false (apply println args)))

(declare crawl)

(defn- crawl-let [bindings forms [fname task-name :as args]]
  (log "crawl-simple-let" bindings forms fname task-name)
  (if (empty? bindings)
    ;; no reason to keep this let around
    ;; emit a do just in case (might need to run this more than once..
    `(do ~@(map crawl forms))

    `(let
       ~(make-bindings (map first bindings) (map second bindings))
       ~@(map crawl forms))))

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

(defn simplify-ast [expr]
  "
  applies some light simplification to the syntax tree
  (useful in trying to stave off stack overflow)
  "
  (log "simplify-ast" expr)
  (crawl expr))
