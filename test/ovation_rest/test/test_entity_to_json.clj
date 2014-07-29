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

(facts "about augmeting entity DTO"
         (fact "adds self link"
               (let [id (UUID/randomUUID)]
                 (.get (.get (util/augment-entity-dto (.. (DtoBuilder. "Project" id) (build))) "links") "self") => #{(format "ovation://entities/%s" (str id))})))
