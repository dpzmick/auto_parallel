(ns com.dpzmick.runtime.fork-join-par
  (:import (java.util.concurrent RecursiveTask)))

(defn- log [& args] (if false (apply println args)))

(defn new-task
  "
  creates new fork/join task to compute the function f
  "
  [f]
  (proxy [RecursiveTask] []
    (compute [] (f))))

(defn fork [^RecursiveTask t] (log "fork") (.fork ^RecursiveTask t))
(defn join [^RecursiveTask t] (log "join") (.join ^RecursiveTask t))
