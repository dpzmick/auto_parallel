(ns auto-parallel.core
  (:gen-class))

;; make it easier to play with syntax tree
(defn args [expr] (rest expr))
(defn fun  [expr] (first expr))
(defn const? [expr] (not (list? expr)))
(defn all-args-constant?
  [expr]
  (every? true? (map const? (args expr))))

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
  "should saturate" ;; this is significantly faster, but not what I want
  []
  (let
    [r      (slow-function 100 #(rand-int 100))
     normal (time (+ (r) (+ (r) (r))))
     par    (time (par1 (+ (r) (+ (r) (r)))))]
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

