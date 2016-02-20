(ns com.dpzmick.parallel-macros.parlet
  (:require [com.dpzmick.ast-manip.dependency  :refer :all]
            [com.dpzmick.ast-manip.replace     :refer :all]
            [com.dpzmick.util                  :refer :all]
            [com.dpzmick.runtime.fork-join-par :as p]))

(def dependency-error
  "this let form cannot be parallel. There are dependencies in the bindings")

;; this can't be a noop when there is only one val, there are some things that
;; will generate parlets with only one value
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
      `(let
         ~(make-bindings names tasks)
         ~@(map #(replace-many names new-vals %) forms)))))
