(ns com.dpzmick.ast-manip.ast-crawl)

(def not-supported "this form is not supported")

(defn defhandlerhelper
  ([a _ b _ c] (defhandlerhelper a _ b _ c _ []))
  ([hname _ cb-name _ cb-args _ bindings]
   `(defn ~hname ~['expr 'callbacks 'callback-args]
      (if (not (contains? ~'callbacks ~cb-name))
        (throw (Exception. (str "no callback named " ~cb-name " defined"))))
      (let ~bindings
        ((~cb-name ~'callbacks) ~@(concat cb-args ['callback-args]))))))

(defmacro defhandler [& args] (apply defhandlerhelper args))

(defmacro defmanyhandlers [handler-names _ cb-names _ handler-args]
  `(do
     ~@(map (fn [handler-name cb-name]
              `(defhandler            ~handler-name
                 ~'for-callback-named ~cb-name
                 ~'using-arguments    ~handler-args))
            handler-names cb-names)))

;; define handlers for each type of expression
;; callbacks are called with:
;; args specified ++ callback-args (from ast-crawl-expr)
(defhandler let-handler
  for-callback-named :let-cb
  use-arguments (bindings forms)
  where [bindings (partition 2 (second expr))
         forms    (rest (rest expr))])

(defhandler map-handler
  for-callback-named :map-cb
  use-arguments (k v)
  where [k (keys expr)
         v (vals expr)])

(defmanyhandlers (vector-handler list-handler const-handler)
  for-callbacks-named (:vector-cb :list-cb :const-cb)
  all-using-arguments (expr))

(defn- const? [expr] (and (not (sequential? expr)) (not (map? expr))))

(defn ast-crawl-expr
  "
  Crawls the AST for a given expression, calling the callback functions with
  callback-args on each subexpression.
  "
  ([expr callbacks] (ast-crawl-expr expr callbacks nil))
  ([expr callbacks callback-args]
   (cond
     (const? expr)          (const-handler  expr callbacks callback-args)
     (= 'let* (first expr)) (let-handler    expr callbacks callback-args)
     (vector? expr)         (vector-handler expr callbacks callback-args)
     (sequential? expr)     (list-handler   expr callbacks callback-args)
     (map? expr)            (map-handler    expr callbacks callback-args)
     :else                  (throw (Exception. (str not-supported expr))))))
