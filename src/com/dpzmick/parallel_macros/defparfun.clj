(ns com.dpzmick.parallel-macros.defparfun
  (:require [com.dpzmick.ast-manip.dependency :refer :all]
            [com.dpzmick.util :refer :all]
            [com.dpzmick.ast-manip.replace :refer :all]
            [com.dpzmick.ast-manip.simplify-ast :refer :all]
            [com.dpzmick.ast-manip.expand-lets :refer :all]
            [com.dpzmick.ast-manip.calls-to-tasks :refer :all]
            [com.dpzmick.ast-manip.bb-edit :refer :all]))

(defn- add-do [forms] `(do ~forms))

(defn- defparfun-helper [fname args forms grain]
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
    `(defn ~fname ~args
       ~(if (not (nil? grain))
          ;; need to emit a granularity check
          ;; this up will often create unreachable code, but that's okay
          `(if ~grain
             ~forms
             ~body)))))

;; can't treat functions like first class objects here
;; the macro can only parallelize from the call site, if the call site is
;; actually the call site
;; TODO get & forms] to cooperate
(defmacro defparfun
  ([fname args forms] (defparfun-helper fname args forms nil))
  ([fname args grain forms] (defparfun-helper fname args forms grain)))

