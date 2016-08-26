(ns one.love.command.ast
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [hara.string.case :as case]
            [clojure.walk :as walk])
  (:import java.util.jar.JarFile
           com.rethinkdb.gen.ast.Datum)
  (:refer-clojure :exclude [>]))

(defn rethink-ast
  "reads all ast-classes from the `com.rethinkdb.gen.ast` package"
  {:added "0.1"} []
  ;; rethinkdb-ast.edn generated using `lein run -m gen-ast-list/generate`
  (->> (read-string (slurp (io/resource "rethinkdb-ast.edn")))
       (map (fn [class-name]
              [(-> class-name case/spear-case keyword)
               (Class/forName (str "com.rethinkdb.gen.ast." class-name))]))
       (into {})))

(defn datumise [m]
  (reduce-kv (fn [out k v]
               (let [k (if (keyword? k) (name k) k)
                     v (if (instance? Datum v) v (Datum. v))]
                 (assoc out k v)))
             {}
             m))

(defmacro create-term-form
  "creates a term form from a class name"
  {:added "0.1"}
  [name cls]
  `(defn ~name
     ([] (~name [] {}))
     ([~'args] (~name ~'args {}))
     ([~'args ~'opts]
      (new ~cls
           (com.rethinkdb.model.Arguments. (walk/stringify-keys ~'args))
           (com.rethinkdb.model.OptArgs/fromMap
            (datumise ~'opts))))))

(defn create-term-fn
  "create a term function from a class name"
  {:added "0.1"} [k cls]
  (eval (list `create-term-form
              (->> (name k)
                   (str "ast-")
                   symbol)
              cls)))

(defonce classes
  (dissoc (rethink-ast)
          :reql-function0
          :reql-function1
          :reql-function2
          :reql-function3
          :reql-function4
          :reql-expr
          :reql-ast
          :datum
          :func))

(defonce fns (reduce-kv (fn [out k v]
                          (assoc out k (create-term-fn k v)))
                        {}
                        classes))
