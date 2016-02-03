(ns com.dpzmick.parallel-macros.parlet
  (:require [com.dpzmick.ast-manip.dependency  :refer :all]
            [com.dpzmick.ast-manip.replace     :refer :all]
            [com.dpzmick.util                  :refer :all]
            [com.dpzmick.runtime.fork-join-par :as p]))

(def dependency-error
  "this let form cannot be parallel. There are dependencies in the bindings")

(defmacro parlet
  [bindings & forms]
  (if (let-has-deps? bindings)
    (throw (Exception. dependency-error))

    (let
      [pairs    (partition 2 bindings)
       names    (map first pairs)
       vals     (map second pairs)
       tasks    (vec (map (fn [v] `(p/fork (p/new-task (fn [] ~v)))) vals))
       new-vals (map (fn [n] `(p/join ~n)) names)]

      ;; each val becomes a fork join task, each reference to the value becomes
      ;; (join task)

      ;; use pattern matching to express this
      `(let
         ~(make-bindings names tasks)
         ~@(map #(replace-many names new-vals %) forms)))))
