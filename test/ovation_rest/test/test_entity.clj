(ns ovation-rest.test.test_entity
  (:use midje.sweet)
  (:import (us.physion.ovation.domain DtoBuilder URIs)
           (java.util UUID))
  (:require [ovation-rest.util :as util]
            [ovation-rest.interop :refer [clojurify]]
            [ovation-rest.entity :as entity]))



(facts "about augmeting entity DTO"
         (fact "adds self link"
               (let [id (UUID/randomUUID)]
                 (->> (util/augment-entity-dto (clojurify (.. (DtoBuilder. "Project" id) (build)))) (:links) (:self)) => #{(format "ovation://entities/%s" (str id))})))

(facts "about entity handlers"
       (facts "index-resource"
              (fact "gets projects"
                    (entity/index-resource ...apikey... "project" ...hosturl...) => ...maps...
                    (provided
                      (util/ctx ...apikey...) => ...ctx...
                      (#'ovation-rest.entity/get-projects ...ctx...) => ...entities...
                      (util/into-map-array ...entities... ...hosturl...) => ...maps...))))
