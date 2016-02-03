(ns com.dpzmick.parallel-macros.defparfun
  (:require [com.dpzmick.ast-manip.dependency :refer :all]
            [com.dpzmick.ast-manip.replace :refer :all]
            [com.dpzmick.ast-manip.simplify-ast :refer :all]
            [com.dpzmick.ast-manip.expand-lets :refer :all]
            [com.dpzmick.ast-manip.calls-to-tasks :refer :all]
            [com.dpzmick.ast-manip.bb-edit :refer :all]))

(defn add-do [forms] `(do ~forms))

;; can't treat functions like first class objects here
;; the macro can only parallelize from the call site, if the call site is
;; actually the call site
(defmacro defparfun [fname args forms]
  (let
    [body (-> (add-do forms)
              ;; expand lets to get AST in a form which is easy to understand
              expand-lets
              (move-calls-to-header fname)

              (simplify-ast)
              (simplify-ast) ;; catches some new things

              (calls-to-tasks fname)

              ; (simplify-ast)
              )]


    ;; define the function that users will actually call
    `(defn ~fname ~args ~body)))
