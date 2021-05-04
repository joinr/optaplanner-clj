(ns optaplanner-clj.util
  (:import [java.util.function Function BiFunction]
           [org.optaplanner.core.api.function
            TriFunction TriPredicate
            QuadFunction QuadPredicate
            PentaFunction PentaPredicate]))

(def funcmap
  {0 'java.util.function.Function
   1 'java.util.function.BiFunction
   2 'org.optaplanner.core.api.function.TriFunction
   3 'org.optaplanner.core.api.function.QuadFunction
   4 'org.optaplanner.core.api.function.PentaFunction})

(defmacro method-ref
  "Expands into code that creates a fn that expects to be passed an
  object and any args and calls the named instance method on the
  object passing the args. Use when you want to treat a Java method as
  a first-class fn. name may be type-hinted with the method receiver's
  type in order to avoid reflective calls.
  Implements java.lang.Function and optaplanner's derivatives up to
  PentaFunction for 5 arity functions."
  {:added "1.0"}
  [name & args]
  (let [t (with-meta (gensym "target")
            (meta name))
        n (count args)
        body `(. ~t (~name ~@args))]
    `(reify ~'clojure.lang.IFn
       (~'invoke [this# t# ~@args]
        (let [~t t#]
          ~body))
       ~@(if-let [iface (funcmap n)]
          `(~iface
            (~'apply [this# t# ~@args]
             (let [~t t#]
               ~body)))
          (throw (ex-info "Supports arg counts from 1..5!" {:method name}))))))
