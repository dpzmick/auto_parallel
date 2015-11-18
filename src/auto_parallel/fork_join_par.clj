(ns auto-parallel.fork-join-par
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
  Sort of confusing behavior.
  Starts a ForkJoin task for each element in the sequence, waits for the first
  to finish, then returns. The rest will be computed lazily in the background.
  "
  [f lst]
  (let
    [exprs  (map #(fn [] (f %)) lst)
     tasks  (map new-task exprs)
     forked (doall (map fork (rest tasks))) ;; need to force them all to start
     me     (compute (first tasks))]
    (cons me (map join forked)))) ;; because this map is lazy

(defn pcalls
  [& fs]
  (pmap #(%) fs))

(defmacro pvalues
  [& exprs]
  `(pcalls ~@(for [e exprs] `(fn [] ~e))))
