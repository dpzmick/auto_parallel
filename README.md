# auto_parallel
Collection of clojure macros to parallelize code. This project is an experiment
which will attempt to allow for simple parallelization of existing code, in
cases where builtins like pmap, or libraries like reducers, tesser, and
claypoole don't quite fit.

## goals
* parallelize code with very little thought (just stick stuff all over the
  codebase without really thinking and maybe get a speedup).
* threadpools
* be predictable

## big TODOS
* support map syntax

# features
## parlet macro
A parallel let expression. Each of the let bindings is evaluated in a fork/join
task. The tasks are not joined until the values are needed.

In the following examples "p" refers the fork_join_par namespace in this
project.

Simple example (cleaned up for readability):

    (macroexpand-1 '(parlet [a 1 b 2 c 3] (+ a b c)))

    (let
     [[a b c]
      [(p/fork (p/new-task (fn [] 1)))
       (p/fork (p/new-task (fn [] 2)))
       (p/fork (p/new-task (fn [] 3)))]]
     (+
      (p/join a)
      (p/join b)
      (p/join c)))

The join call is saved until the moment the value is needed, to allow code like
this:

    (macroexpand-1 '(parlet [a (long-fun 100)
                            b (long-fun 100)
                            c (long-fun 100)]
                        (other-long-fun-with-effects 200)
                        (+ a b c)))

We want to allow the other-long-fun-with-effects call to proceed even if all of
the values in the parlet haven't finished. The code generated in this case:

    (let
     [[a b c]
      [ (p/fork (p/new-task (fn [] (long-fun 100))))
        (p/fork (p/new-task (fn [] (long-fun 100))))
        (p/fork (p/new-task (fn [] (long-fun 100))))]]
     (other-long-fun-with-effects 200)
     (+ (p/join a) (p/join b) (p/join c)))

The real value of this behavior will become evident when the parexpr macro is
discussed.


### error detection
The parlet macro detects dependencies. For example, the compiler shouldn't let
us attempt to make this let parallel:

    (parlet [a 1 b (+ a 1) b])

    Exception this let form cannot be parallel. There are dependencies in the bindings

As you can see, it doesn't let us!

##parexpr macro
(currently broken, probably)
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

