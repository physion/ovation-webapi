(ns ovation-rest.interop
  (:import (java.util Collection List Set Map)
           (com.google.common.collect Multimap)))

(defmulti clojurify class)

(defmethod clojurify Map [m]
  "Converts a java.util.Map into a Clojure map"
  (into {} (map (fn [entry]
                    (let [[k v] entry]
                      [(keyword k) (clojurify v)])) m)))

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

