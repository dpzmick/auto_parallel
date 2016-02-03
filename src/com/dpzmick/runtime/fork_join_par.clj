(ns com.dpzmick.runtime.fork-join-par
  (:import (java.util.concurrent RecursiveTask)))

(defn new-task
  "
  creates new fork/join task to compute the function f
  "
  [f]
  (proxy [RecursiveTask] []
    (compute [] (f))))

(defn fork [t] (.fork t))
(defn join [t] (.join t))
(defn compute [t] (.compute t))
