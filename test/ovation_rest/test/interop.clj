(ns ovation-rest.test.interop
  (:import (com.google.common.collect HashMultimap)
           (java.util HashMap HashSet LinkedList))
  (:use midje.sweet)
  (:require [ovation-rest.interop :refer [clojurify]]))

(facts "about Java interop"
       (facts "about collections"
              (fact "converts Java Map to clojure Map"
                    (let [jmap (doto (HashMap.)
                                 (.put "key1" "value1"))]
                      (clojurify jmap)) => {:key1 "value1"})
              (fact "converts Java Set to Clojure Set"
                    (clojurify (doto (HashSet.) (.add "abc") (.add "def"))) => #{"abc" "def"})
              (fact "converts Java List to a Clojure linked list"
                    (clojurify (doto (LinkedList.) (.add "abc") (.add "def"))) => ["abc" "def"])
              (fact "converts a Guava Multimap to a Clojure Map of Sets"
                    (let [mm (doto (. HashMultimap create)
                               (.put "key1" "value1.1")
                               (.put "key1" "value1.2")
                               (.put "key2" "value2.1")
                               (.put "key3" "value3.1"))]
                      (clojurify mm) => {:key1 #{"value1.1" "value1.2"}
                                         :key2 #{"value2.1"}
                                         :key3 #{"value3.1"}})))
       (facts "about compound collections"
              (fact "converts Map of Multimaps to clojure Map of Maps"
                    (let [mm (doto (. HashMultimap create)
                               (.put "key1" "value1.1")
                               (.put "key1" "value1.2")
                               (.put "key2" "value2.1")
                               (.put "key3" "value3.1"))
                          m (doto (HashMap.)
                              (.put "multi" mm))]
                      (clojurify m) => {:multi {:key1 #{"value1.1" "value1.2"}
                                                :key2 #{"value2.1"}
                                                :key3 #{"value3.1"}}}))))
