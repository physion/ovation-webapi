(ns ovation-rest.test.test_entity_to_json
  (:use midje.sweet)
  (:import (us.physion.ovation.domain DtoBuilder))
  (:require [ovation-rest.util :as util]))

(defn json-to-object-array [json]
  (->
    (new com.fasterxml.jackson.databind.ObjectMapper)
    (.registerModule (com.fasterxml.jackson.datatype.guava.GuavaModule.))
    (.registerModule (com.fasterxml.jackson.datatype.joda.JodaModule.))
    (.readValue json java.util.List)
    ))

(defn parse-json [json-string-seq]
  (json-to-object-array json-string-seq))

(facts "about entity-to-JSON conversion"
       (fact "adds self link"
             (parse-json (util/entities-to-json [...entity...])) => {"type" "Project" "attributes" {}}
             (provided
               (util/entity-to-map ...entity...) => (-> (DtoBuilder. "Project")
                                                        (.build)))))
