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
###parags macro
parexpr is a macro that executes an expression in parallel. It evaluates any
subexpressions with no dependencies first.

    (def r1 (slow-function 100 #(rand-int 100)))
    (def r2 (slow-function 500 #(rand-int 100)))

    (time (+ (r2) (+ (r1) (r1))))
    "Elapsed time: 710 msecs"

    (time (parargs (+ (r2) (+ (r1) (r1)))))
    "Elapsed time: 504 msecs"

    ;; evalute (r2) (r1) (r1) at the same time, so the execution takes as long
    ;; as the longest subexpression, which is (r2)

# notes and interesting issues
## claypoole futures
claypoole futures are great, but you can't be careless with them

    (def pool (cp/threadpool 1))
    (defn f2 [] @(cp/future pool 1)) ;; create and deref future on threadpool returuning 1
    (defn f1 [] @(cp/future pool (f2))) ;; create and deref future that calls f2

    (d1) ;; deadlock. the first future uses the whole pool, the second one can never run

This means that, if we want to be able to throw parexpr wherever we want, we
need to do something more clever. For example, this deadlocks as well

    (def pool (cp/threadpool 1))
    (defn f2 [] (parexpr pool (+ 1 2)))
    (defn f1 [] (parexpr pool (+ (f2) (f2))))
    (f1) ;; deadlock, no available threads to evaluate the futures
