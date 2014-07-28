(ns ovation-rest.test.test_entity_to_json.clj
  (:import (us.physion.ovation.domain DtoBuilder)))
  (:require [midje.sweet :refer :all])
  (:require [ovation-api-webservice.util :as util])
  (:import (us.physion.ovation.domain DtoBuilder))

(defn parse-json [json-string-seq]
  (map util/json-to-object json-string-seq))

(facts "about entity-to-JSON conversion"
       (fact "adds self link"
             (parse-json (util/entities-to-json [...entity...])) => {"type" "Project" "attributes" {}}
             (provided
               (util/entity-to-map ...entity...) => (-> (DtoBuilder. "Project")
                                                        (.build)))))
