(ns auto-parallel.calls-to-tasks
  (:require [auto-parallel.replace :refer :all]
            [auto-parallel.ast-crawl :refer :all]
            [auto-parallel.fork-join-par :as fj]
            [clojure.pprint :refer :all]
            [auto-parallel.util :refer :all]
            [clojure.walk :refer :all]))

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

(defn- find-interesting-bindings [bindings fname]
  (filter
    (fn [[n v]]
      (immediate-call? v fname))
    bindings))

(defn- crawl-let [bindings forms [fname task-name :as args]]
  (log "crawl-calls-let" bindings forms fname task-name)
  (let
    [interesting   (find-interesting-bindings bindings fname)

     ;; deal with the bindings
     ;; doing it this way to prevent having to deal with dependencies or run
     ;; expand-lets yet again
     new-bindings-names (map first bindings)
     new-bindings-vals  (map #(if (immediate-call? % fname)
                                (replace-all fname task-name %)
                                %)
                             (map second bindings))

     ;; deal with the body
     added-forms   (map
                     (fn [[n v]] (list `fj/fork n))
                     interesting)

     replacement-pairs (map
                         (fn [[n v]] (list n (list `fj/join n)))
                         interesting)

     replaced-forms (map #(replace-many
                            (map first replacement-pairs)
                            (map second replacement-pairs)
                            %)
                         (map #(crawl % args) forms))]

    `(let
       ~(make-bindings new-bindings-names new-bindings-vals)
       ~@(concat added-forms replaced-forms))))


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

(defn- crawl [expr [fname task-name :as args]]
  (ast-crawl-expr
    (macroexpand-all expr)
    {
     :let-cb crawl-let
     :vector-cb crawl-vector
     :list-cb crawl-list
     :const-cb crawl-const
     }
    args))

(defn calls-to-tasks [expr fname task-name]
  "
  this function processes let expressions.
  For every call to the function fname in the bindings of the let expression,
  the call is replaced with a call to task-name.

  In the body of the let, lines are added to fork each of these tasks,
  then all references to the tasks are replaced with (join task)

  Run this after running the bb-edit function, to convert a function to a
  parallel function.

  Any call not an 'immediate' binding in a let expression
  (eg [a (foo a)] counts but [a (c (foo a))] doesn't) will be ignored
  "
  (log "calls-to-tasks" expr fname task-name)
  (crawl expr [fname task-name]))
