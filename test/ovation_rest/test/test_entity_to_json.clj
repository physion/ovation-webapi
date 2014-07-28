(ns ovation-rest.test.test_entity_to_json
  (:use midje.sweet)
  (:import (us.physion.ovation.domain DtoBuilder URIs)
           (java.util UUID))
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
             (let [id (UUID/randomUUID)]
               (.get (first
                       (parse-json (util/entities-to-json [...entity...]))) "links") => {"self" [(format "ovation://entities/%s" (str id))]}
               (provided
                 (util/entity-to-map ...entity...) => (-> (DtoBuilder. "Project" id)
                                                          ;(.withLink "rel" (URIs/create (UUID/randomUUID)))
                                                          (.build))))))
