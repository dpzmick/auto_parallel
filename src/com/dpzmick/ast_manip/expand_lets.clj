(ns com.dpzmick.ast-manip.expand-lets
  (:require [com.dpzmick.ast-manip.ast-crawl :refer :all]
            [com.dpzmick.util :refer :all]
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

;; almost an identity
(defn el-map   [ks vs _]
  (zipmap
    (map expand-lets ks)
    (map expand-lets vs)))

;; basically identity function
(defn el-seq   [expr _] (map expand-lets expr))
(defn el-vec   [expr _] (mapv expand-lets expr))
(defn el-const [expr _] expr)

(defn expand-lets
  "
  defines a pass which will expand every let into a let with a single binding
  "
  [expr]
  (ast-crawl-expr
    (macroexpand-all expr)
    {
     :let-cb    el-let
     :vector-cb el-vec
     :list-cb   el-seq
     :map-cb    el-map
     :const-cb  el-const
     }))

(defmacro test-expand-lets [expr] (expand-lets expr))
