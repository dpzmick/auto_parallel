(ns auto-parallel.bb-edit-test)

;; test for nested function calls (fib (fib 10)), the bindings emitted will have
;; a dependency, need to make sure they get emitted in the right order
