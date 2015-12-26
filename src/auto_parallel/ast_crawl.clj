(ns auto-parallel.ast-crawl)

;; TODO map syntax
(defn ast-crawl-expr
  ([expr callbacks] (ast-crawl-expr expr callbacks nil))
  ([expr callbacks callback-args]
   "
   Crawls the AST for a given expression, calling the callback functions with
   callback-args on each subexpression.

   callbacks:
   - let-cb
   - vector-cb
   - sequential-cb
   - const-cb
   "
   (if (sequential? expr)
     ;; we have a function call or some other form (like a vector or list
     ;; literal)
     (cond
       (= 'let* (first expr)) ((:let-cb callbacks) expr callback-args)
       (vector? expr)         ((:vector-cb callbacks) expr callback-args)
       (seq? expr)            ((:sequential-cb callbacks) expr callback-args)
       :else                  (throw (Exception. "this form is not supported")))

     ;; the expression is a single term
     ((:const-cb callbacks) expr callback-args))))
