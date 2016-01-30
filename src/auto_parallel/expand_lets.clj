(ns auto-parallel.expand-lets
  (:require [auto-parallel.ast-crawl :refer :all]
            [auto-parallel.util :refer :all]
            [clojure.pprint :refer :all]
            [clojure.walk :refer :all]))

(declare expand-lets)

(defn el-let [bindings forms _]
  (if (empty? bindings)
    `(do
       ~@(map expand-lets forms))

    (let
      [bname (first (first bindings))
       bval  (second (first bindings))
       bvexp (expand-lets bval)]
      `(let
         [~bname ~bvexp]
         ~(el-let (rest bindings) forms _)))))

;; basically identity function
(defn el-seq   [expr _] (map expand-lets expr))
(defn el-vec   [expr _] (mapv expand-lets expr))
(defn el-const [expr _] expr)

(defn expand-lets [expr]
  "
  defines a 'pass' which will expand every let into a let with a single binding
  "
  (ast-crawl-expr
    (macroexpand-all expr)
    {
     :let-cb    el-let
     :vector-cb el-vec
     :list-cb   el-seq
     :const-cb  el-const
     }))

(defmacro test-expand-lets [expr] (expand-lets expr))
