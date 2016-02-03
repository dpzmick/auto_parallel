(ns com.dpzmick.ast-manip.calls-to-tasks
  (:require [com.dpzmick.ast-manip.replace :refer :all]
            [com.dpzmick.ast-manip.ast-crawl    :refer :all]
            [com.dpzmick.util                   :refer :all]
            [com.dpzmick.parallel-macros.parlet :refer [parlet]]
            [com.dpzmick.runtime.fork-join-par  :as fj]
            [clojure.pprint                     :refer :all]
            [clojure.walk                       :refer :all]))

(defn- log [& args] (if false (apply println args)))

(declare crawl)

(defn- immediate-call? [expr fname]
  (log "immediate-call?" expr fname)
  (if (sequential? expr)
    (do
      (log "immediate-call? sequential")
      (= (first expr) fname))
    (do
      (log "immediate-call? not sequential?")
      false)))

(defn- crawl-let [bindings forms fname]
  (log "crawl-calls-let" bindings forms fname)
  (let [forms (map #(crawl % fname) forms)
        names    (map first bindings)
        values   (map second bindings)
        ;; maybe it wasn't such a good idea to extract the bindings in this way
        gross-bindings (make-bindings names values)]

    (if (every? #(immediate-call? (second %) fname) bindings)
      `(parlet ~gross-bindings ~@forms)
      `(let    ~gross-bindings ~@forms))))

;; everything is the identify other than let
(defn- crawl-vector [expr args]
  (log "crawl-calls-vector" expr args)
  (mapv #(crawl % args) expr))

(defn- crawl-list   [expr args]
  (log "crawl-calls-list" expr args)
  (map  #(crawl % args) expr))

(defn- crawl-const  [expr _]
  (log "crawl-calls-const" expr)
  expr)

(defn- crawl [expr fname]
  (ast-crawl-expr
    (macroexpand-all expr)
    {
     :let-cb crawl-let
     :vector-cb crawl-vector
     :list-cb crawl-list
     :const-cb crawl-const
     }
    fname))

(defn calls-to-tasks
  "
  this pass processes let expressions.

  every let expression whose bindings are all immediate calls to fname will be
  converted to a parlet

  Any call not an 'immediate' binding in a let expression
  (eg [a (foo a)] counts but [a (c (foo a))] doesn't) will be ignored

  Run this after running the bb-edit pass, to convert a function to a
  parallel function.
  "
  [expr fname]
  (log "calls-to-tasks" expr fname)
  (crawl expr fname))
