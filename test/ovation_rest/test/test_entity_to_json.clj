(ns ovation-rest.test.test_entity_to_json
  (:use midje.sweet)
  (:import (us.physion.ovation.domain DtoBuilder URIs)
           (java.util UUID))
  (:require [ovation-rest.util :as util]
            [ovation-rest.interop :refer [clojurify]]))


(facts "about augmeting entity DTO"
         (fact "adds self link"
               (let [id (UUID/randomUUID)]
                 (->> (util/augment-entity-dto (clojurify (.. (DtoBuilder. "Project" id) (build)))) (:links) (:self)) => #{(format "ovation://entities/%s" (str id))})))
