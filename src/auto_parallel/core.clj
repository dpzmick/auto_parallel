(ns auto-parallel.core
  (use [clojure.tools.trace]))

;; make it easier to play with syntax tree
(defn args [expr] (rest expr))
(defn fun  [expr] (first expr))
(defn const? [expr] (not (seq? expr)))
(defn all-args-const?
  [expr]
  (every? true? (map const? (args expr))))

; https://gist.github.com/jcromartie/5459350
(defmacro parlet
  [bindings & forms]
  (let
    [pairs (partition 2 bindings)
     names (map first pairs)
     vals  (map second pairs)]
    `(let [[~@names] (pvalues ~@vals)] ~@forms)))

;; everything this recovers in :names can be computed in parallel
;; call this multiple times to get multiple parallelize able steps
;; assumes everything is a function call
;; assumes all constants have been eliminated already
(defn prune
  "prunes a single level of leaves from syntax tree"
  [expr]
  (cond
    (const? expr)
    {:expr expr, :names {}}

    (all-args-const? expr)
    (let
      [n (gensym "expr")]
      {:expr n, :names {n expr}})

    :else
    (let
      [subresults (map prune (args expr))
       subexprs   (map :expr subresults)
       subnames   (map :names subresults)
       mynames    (apply merge subnames)]
      {:expr (cons (fun expr) subexprs), :names mynames})))

(defn make-nested-lets
  [expr]
  (if (const? expr)
    expr
    (let
      [{e :expr n :names} (prune expr)
       bindings           (apply vector (apply concat (into (list) n)))]
      `(parlet ~bindings ~(make-nested-lets e)))))


(defmacro par2 [expr] (make-nested-lets expr))

(defn naive-parallel
  [expr]
  (if (const? expr)
    `(future ~expr)
    (let
      [names        (take (count (args expr)) (repeatedly #(gensym "expr")))
       values       (map naive-parallel (args expr))
       let-bindings (apply concat (into (list) (zipmap names values)))]

      `(future
            (let
                  ~(apply vector let-bindings)
                  ~(cons (fun expr) (map #(list 'deref %) names)))))))

(defmacro par1 [expr] `@ ~(naive-parallel expr))

(defn slow-function
  "returns a function which waits for a while, then returns the result of the no argument function"
  [how-slow fun]
  (fn []
    (do
      (Thread/sleep how-slow)
      (fun))))

(defn experiment1
  "should run faster with par macro than without"
  []
  (let
    [r      (slow-function 100 #(rand-int 100))
     normal (time (+ (r) (r)))
     par    (time (par1 (+ (r) (r))))]
    nil))

(defn blah
  "should saturate"
  []
  (let
    [r      (slow-function 100 #(rand-int 100))
     normal (time (+ (r) (+ (r) (r))))
     par    (time (par1 (+ (r) (+ (r) (r)))))]
    nil))

(defn blah2
  "should saturate"
  []
  (let
    [r      (slow-function 100 #(rand-int 100))
     par1    (time (par1 (+ (r) (+ (r) (r)))))
     par2    (time (par2 (+ (r) (+ (r) (r)))))]
    nil))

(defn experiment2
  "how big is performance hit?" ;; very
  []
  (let
    [normal (time (+ (rand-int 100) (rand-int 100)))
     par    (time (par1 (+ (rand-int 100) (rand-int 100))))]
    nil))

(defn macropprint [expr] (pprint (macroexpand expr)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

