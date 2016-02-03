;; what a mess

(ns com.dpzmick.ast-manip.bb-edit
  (:require [com.dpzmick.ast-manip.ast-crawl :refer :all]
            [com.dpzmick.util :refer :all]
            [clojure.walk :refer [macroexpand-all]]))

(def multiple-bindings
  "this let has multiple bindings, please run expand-lets first")

(defn- log [& args] (if false (apply println args)))

(declare crawl)

;; helpful
(defn- conditional [expr] (first (rest expr)))
(defn- if-true     [expr] (first (rest (rest expr))))
(defn- if-false    [expr] (first (rest (rest (rest expr)))))

(defn- handle-forms [forms f]
  (log "handle-forms" forms f)
  (let
    [forms-crawled  (map #(crawl % f) forms)
     forms-bindings (mapcat :bindings forms-crawled)
     forms-names    (map first forms-bindings)
     forms-values   (map second forms-bindings)
     forms-forms    (map :forms forms-crawled)]

    (if (empty? forms-bindings)
      ;; if there are no bindings, just stick the forms
      ;; we still need to use forms-forms because these might have been modified
      ;; in some way in the recursion, even without bindings
      `(do ~@forms-forms)

      ;; there they are bindings, bind then, then stick in the new forms
      `(let
         ~(make-bindings forms-names forms-values)
         ~@forms-forms))))

;; assume that all lets have a single binding, need to run expand-lets first
;; since lets only have a single binding, any binding emitted by the evaluation
;; of the values can be put into a new let coming before this let
;; any bindings coming from the evaluation of the forms can be placed in a new
;; let following the existing let (or at the end of this let)
(defn- crawl-let [bindings forms f]
  (log "crawl-let" bindings forms f)
  (if-not (= 1 (count bindings))
    (throw (Exception. multiple-bindings)))

  (let
    [names          (map first bindings)
     values-crawled (map #(crawl % f) (map second bindings))
     values-bs      (mapcat :bindings values-crawled)
     values-forms   (map :forms values-crawled)]


    (if (empty? values-bs)
      ;; if there are no bindings coming from the values, just emit the same let
      ;; again, and handle the forms
      {
       :bindings []
       :forms
      `(let
         ~(make-bindings names (map second bindings))
         ~(handle-forms forms f))
       }

      ;; if there are bindings, emit a let to handle those bindings, then emit
      ;; the restructured
      {
       :bindings []
       :forms
       `(let
          ~(make-bindings (map first values-bs) (map second values-bs))
          (let
            ~(make-bindings names values-forms)
            ~(handle-forms forms f)))
       }
      )))


(defn- crawl-list [expr f]
  (log "crawl-list" expr f)
  (cond
    ;; found flow control
    (= (first expr) 'if)
    (let
      ;; we already crawled all of the bindings here, so don't worry about them
      [it-crawl    (crawl (if-true expr)  f)
       it-bindings (:bindings it-crawl)
       it-forms    (:forms it-crawl)
       it-names    (map first it-bindings)
       it-values   (map second it-bindings)

       if-crawl    (crawl (if-false expr) f)
       if-bindings (:bindings if-crawl)
       if-forms    (:forms if-crawl)
       if-names    (map first if-bindings)
       if-values   (map second if-bindings)]

      ;; return the {bindings, forms} dict
      {
       :bindings []
       :forms

       ;; include the basic block level in the new header for debugging
       `(if ~(conditional expr)
          (let ~(make-bindings it-names it-values) ~it-forms)
          (let ~(make-bindings if-names if-values) ~if-forms))
       })

    ;; found one of these function calls
    ;; we need to move this entire expression to the header for this block, and
    ;; instead insert a name
    ;; crawl all of the arguments here so we don't have to do it in that giant
    ;; mess up above
    ;; always put my-binding at the end of the list, in case it depends on a
    ;; previously created binding
    (= (first expr) f)
    (let
      [my-name        (gensym "expr")
       args-crawled   (map #(crawl % f) (rest expr))
       my-expr        (cons f (map :forms args-crawled))
       my-binding     (list my-name my-expr)
       their-bindings (mapcat :bindings args-crawled)]
      {
       :bindings (reverse (cons my-binding their-bindings))
       :forms my-name
       })

    ;; nothing interesting at this level
    ;; fight with the multiple return anti-pattern
    :else
    (let
      [children (map #(crawl % f) expr)
       all-bs   (mapcat :bindings children)
       forms    (map :forms children)]
      {
       :bindings all-bs
       :forms forms
       })))

(defn- crawl-vector [expr f]
  (log "crawl-vector" expr f)
  (let
    [children          (map #(crawl % f) expr)
     children-bindings (mapcat :bindings children)
     children-forms    (map :forms children)]
    {
     :bindings children-bindings
     :forms (vec children-forms)
     }))

(defn- crawl-const [expr _]
  (log "crawl-const" expr)
  {:bindings [] :forms expr})

(defn- crawl [expr f]
  (log "crawl" (macroexpand-all expr) f)
  (ast-crawl-expr
    (macroexpand-all expr)
    {
     :let-cb    crawl-let
     :vector-cb crawl-vector
     :list-cb   crawl-list
     :const-cb  crawl-const
     }
    f))

(defn move-calls-to-header
  "
  moves all function calls in a ''basic block'' to the header of the basic block

  After running this pass, every call to the provided function will be bound to
  a value in a let expression which has no dependencies on the value returned by
  the function call
  "
  [expr f]
  (let
    [ick      (crawl expr f)
     bindings (:bindings ick)
     names    (map first bindings)
     values   (map second bindings)]

    ;; emit the top level bindings
    (if (empty? bindings)
      `~(:forms ick)
      `(let
         ~(make-bindings names values)
         ~(:forms ick)))))


(defmacro test-move-calls-to-header [f expr] (crawl expr f))
