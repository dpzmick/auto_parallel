(ns auto-parallel.ast-crawl)

(use 'auto-parallel.util)

(defn defhandlerhelper
  ([a _ b _ c] (defhandlerhelper a _ b _ c _ []))
  ([hname _ cb-name _ cb-args _ bindings]
   `(defn ~hname ~['expr 'callbacks 'callback-args]
      (if (not (contains? ~'callbacks ~cb-name))
        (throw (Exception. (str "no callback named " ~cb-name " defined"))))
      (let ~bindings
        ((~cb-name ~'callbacks) ~@(concat cb-args ['callback-args]))))))

(defmacro defhandler [& args] (apply defhandlerhelper args))

;; expands into multiple defhandler statements
(defmacro defmanyhandlers [handler-names _ cb-names _ handler-args]
  `(do
     ~@(map (fn [handler-name cb-name]
              `(defhandler            ~handler-name
                 ~'for-callback-named ~cb-name
                 ~'using-arguments    ~handler-args))
            handler-names cb-names)))

(defhandler let-handler
  for-callback-named :let-cb
  use-arguments (bindings forms)
  where [bindings (partition 2 (second expr))
         forms    (rest (rest expr))])

;; define handlers for each type of expression
;; callbacks will be called with args given ++ callback-args specified in
;; ast-crawl-expr
(defmanyhandlers (vector-handler list-handler const-handler)
  for-callbacks-named (:vector-cb :list-cb :const-cb)
  all-using-arguments (expr))

;; TODO map syntax
(defn ast-crawl-expr
  ([expr callbacks] (ast-crawl-expr expr callbacks nil))
  ([expr callbacks callback-args]
   "
   Crawls the AST for a given expression, calling the callback functions with
   callback-args on each subexpression.
   "
   (if (sequential? expr)
     ;; we have a function call or some other form (like a vector or list
     ;; literal)
     (cond
       (= 'let* (first expr)) (let-handler    expr callbacks callback-args)
       (vector? expr)         (vector-handler expr callbacks callback-args)
       (list? expr)           (list-handler   expr callbacks callback-args)
       :else                  (throw (Exception. "this form is not supported")))

     ;; the expression is a single term
     (const-handler expr callbacks callback-args))))
