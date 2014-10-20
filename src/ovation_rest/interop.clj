(ns ovation-rest.interop
  (:import (java.util Collection List Set Map HashMap)
           (com.google.common.collect Multimap))
  (:require [clojure.walk :refer [stringify-keys]]))

(defmulti clojurify class)

(defmethod clojurify Map [m]
  "Converts a java.util.Map into a Clojure map"
  (into {} (map #(let [[k v] %1]
                  [(keyword k) (clojurify v)]) m)))

(defmethod clojurify Set [s]
  "Converts a java.util.Set into a Clojure set"
  (into #{} s))

(defmethod clojurify List [l]
  "Converts a java.util.List into a Clojure vector"
  (into [] l))

(defmethod clojurify Multimap [mm]
  (clojurify (.asMap mm)))

(defmethod clojurify Collection [c]
  "Generic collection conversion"
  (seq c))

(defmethod clojurify Object [o]
  o)



(defmulti javafy class)
(defmethod javafy Map [m]
  (HashMap. (into {} (map #(let [[k v] %1]
                            [k (javafy v)]) (stringify-keys m)))))
(defmethod javafy Object [o]
  o)
