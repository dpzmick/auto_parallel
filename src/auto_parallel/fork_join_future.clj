(ns auto-parallel.fork-join-future
  (:import java.util.concurrent.ForkJoinPool)
  (:import java.util.concurrent.RecursiveTask))

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

(defn pmap
  "
  Force evaluation of each of the arguments, in parallel, using fork/join tasks.
  "
  [pool f lst]
  (let
    [exprs  (map #(fn [] (f %)) lst)
     tasks  (map new-task exprs)
     forked (doall (map fork (rest tasks)))
     me     (compute (first tasks))]
    (cons me (map join forked))))

(defn pcalls
  [pool & fs]
  (pmap pool #(%) fs))

(defmacro pvalues
  [pool & exprs]
  `(pcalls ~pool ~@(for [e exprs] `(fn [] ~e))))
