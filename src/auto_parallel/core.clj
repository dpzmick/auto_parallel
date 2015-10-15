(ns auto-parallel.core
  (use [clojure.tools.trace]))

;; make it easier to play with syntax tree
(defn args [expr] (rest expr))
(defn fun  [expr] (first expr))
(defn const? [expr] (not (seq? expr)))
(defn all-args-const?
  [expr]
  (every? true? (map const? (args expr))))

;; everything this recovers in :names can be computed in parallel
;; call this multiple times to get multiple parallelize able steps
;; assumes everything is a function call
(defn prune
  "prunes a single level of leaves from syntax tree"
  [expr]
  (if (or (const? expr) (all-args-const? expr))
    (let
      [n (gensym "expr")]
      {:expr n, :names {n expr}})

    (let
      [subresults (map prune (args expr))
       subexprs   (map :expr subresults)
       subnames   (map :names subresults)
       mynames    (apply merge subnames)]
      {:expr (cons (fun expr) subexprs), :names mynames})))

;; TODO all these interfaces are getting wacky
;; outputs an expr and a names-list
;; each element in the names-list is a map, whose bindings can be computed in
;; parallel
(defn prune-all-levels
  ([expr] (prune-all-levels expr (list)))

  ([expr names-list]
   (let
     [{e :expr n :names} (prune expr)]
     (if (const? e)
       {:expr e :names-list (cons n names-list)}
       (recur e (cons n names-list))))))

(defn make-nested-lets
  ([{expr :expr names-list :names-list}] (make-nested-lets expr names-list))
  ([expr names-list]
   (if (empty? names-list)
     expr

     (let
       [bindings (apply vector
                        (apply concat
                               (into (list) (first names-list))))]
       (recur
         `(let ~bindings ~expr)
         (rest names-list))))))

(defn futurize
  [expr]
  (if (const? expr)
    (list 'future expr)

    (list
      'future
      (cons
        (fun expr)
        (map
          #(if (.startsWith (.toString %) "expr")
             (list 'deref %)
             %)
          (args expr))))))

(defn pruned-hack
  [{e :expr n :names-list}]
  {:expr (list 'deref e) :names-list (map futurize-names-map n)})

(defmacro par2 [expr] (make-nested-lets (pruned-hack (prune-all-levels expr))))

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

