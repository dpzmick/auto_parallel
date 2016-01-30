(ns auto-parallel.replace-test)

;; do the macroexpand to give everything the same names
(defmacro defreptest [n e r input output]
  `(deftest ~n
     (is (=
          (macroexpand-all (quote ~output))
          (macroexpand-all (replace-all (quote ~e) (quote ~r) (quote ~input)))))))

(defreptest replace-single a b a b)

(defreptest replace-argument a b (+ a b) (+ b b))

(defreptest replace-let-in-body a b (let [c :d] a) (let [c :d] b))

;; multiple bindings get expanded into multiple lets
(defreptest replace-let-in-body2 a b
  (let [c :d e :f] a)
  (let [c :d] (let [e :f] b)))

;; we end up with extra because of property that makes code easier
(defreptest replace-early-stop a b
  (let [a :b] a)
  (let [a :b] (let [] a)))

(defreptest replace-early-stop-multiple a b
  (let [a :b c :d e :f] a)
  (let [a :b] (let [c :d e :f] a)))

(defreptest replace-nested-lets a b
  (let [c :d] (let [e :f] a))
  (let [c :d] (let [e :f] b)))

(defreptest preserve-vector a b
  [a b c]
  [b b c])
