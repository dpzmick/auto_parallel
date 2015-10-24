# auto_parallel
Collection of clojure macros to parallelize code. There are many existing
parallelization solutions in clojure. This project is an experiment which will
attempt to allow for simple parallelization of existing code, in cases where
builtins like pmap, or libraries like reducers, tesser, and claypoole don't
quite fit.

## goals
* parallelize code with very little though (just stick stuff all over the
  codebase without really thinking and maybe get a speedup).
* threadpools - be predictable

# features
## parallel execution of arguments

    (def r1 (slow-function 100 #(rand-int 100)))
    (def r2 (slow-function 500 #(rand-int 100)))

    (time (+ (r2) (+ (r1) (r1))))
    "Elapsed time: 710 msecs"

    (time (par1 (+ (r2) (+ (r1) (r1)))))
    "Elapsed time: 503 msecs"

    (time (par2 (+ (r2) (+ (r1) (r1)))))
    "Elapsed time: 504 msecs"

# notes and interesting issues
## claypoole futures
claypoole futures are great, but you can't be careless with them

    (def pool (cp/threadpool 1))
    (defn f2 [] @(cp/future pool 1)) ;; create and deref future on threadpool returuning 1
    (defn f1 [] @(cp/future pool (f2))) ;; create and deref future that calls f2

    (d1) ;; deadlock. the first future uses the whole pool, the second one can never run
