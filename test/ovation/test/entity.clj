(ns ovation.test.entity
  (:use midje.sweet)
  (:require [ovation.util :as util]
            [ovation.dao :as dao]
            [ovation.interop :refer [clojurify]]
            [ovation.entity :as entity]
            [ovation.context :as context]
            [ovation.links :as links]
            [com.ashafa.clutch :as cl]
            [ovation.couch :as couch]
            [ovation.version :as ver])
  (:import (us.physion.ovation.data EntityDao$Views)))



(facts "About entities"
  (fact "`of-type` gets all entities by document :type"
    (let [dburl "https://dburl"
          dbuser "db-user"
          dbpass "db-pass"]
      (entity/of-type ...auth... ...resource...) => [...entity1... ...entity2...]
      (provided
        ...auth... =contains=> {:cloudant_key dbuser :cloudant_password dbpass :cloudant_db_url dburl}
        (cl/get-view couch/design-doc EntityDao$Views/ENTITIES_BY_TYPE {:key ...resource... :reduce false :include_docs true}) => [{:doc ...doc1...} {:doc ...doc2...}]
        (couch/transform [...doc1... ...doc2...]) => [...entity1... ...entity2...]
        (entity/filter-trashed [...entity1... ...entity2...] false) => [...entity1... ...entity2...])))

  (fact "`filter-trashed` removes entity docs with :trash_info"
    (entity/filter-trashed [...doc1... ...doc2...] false) => (seq [...doc1...])
    (provided
      ...doc2... =contains=> {:trash_info ...info...}))

  (fact "`get-entities` gets entities by :id"
    (let [dburl "https://dburl"
          dbuser "db-user"
          dbpass "db-pass"]
      (entity/get-entities ...auth... ...ids...) => [...entity1... ...entity2...]
      (provided
        ...auth... =contains=> {:cloudant_key dbuser :cloudant_password dbpass :cloudant_db_url dburl}
        (cl/all-documents {:include_docs true} {:keys ...ids...}) => [{:doc ...doc1...} {:doc ...doc2...}]
        (couch/transform [...doc1... ...doc2...]) => [...entity1... ...entity2...]
        (entity/filter-trashed [...entity1... ...entity2...] false) => [...entity1... ...entity2...])))


  (fact "updates entity attributes"
    (entity/update-entity-attributes ...api... ...id... {:attr1 1
                                                         :attr2 "value"}) => ...result...
    (provided
      (dao/get-entity ...api... ...id...) => ...entity...
      (dao/entity-to-dto ...entity...) => {:_id        "123"
                                           :_rev       "rev1"
                                           :attributes {:attr1 0
                                                        :attr3 "foo"}
                                           :links      {:self "/entity/uri/"}}

      (#'ovation.entity/update-entity ...entity... {:_id        "123"
                                                    :_rev       "rev1"
                                                    :attributes {:attr1 1
                                                                 :attr2 "value"}
                                                    :links      {:self "/entity/uri/"}}) => ...entity...
      (dao/into-seq ...api... '(...entity...)) => ...result...)))

(facts "About entity creation"
  (fact "`-ensure-id` adds random UUID _id"
    (let [id java.util.UUID/randomUUID]

      (entity/-ensure-id {}) => {:_id id}
      (provided
        (entity/make-uuid) => id)))

  (fact "`-ensure-api-version` adds API version"
    (entity/-ensure-api-version {}) => {:api_version ver/schema-version})

  (fact "`-ensure-owner` adds 'owner' relation"
    true=>false)

  (fact "`-add-owner` adds " )

  (fact "`create-entity` inserts a new entity without links"
    (let [dburl "https://dburl"
          dbuser "db-user"
          dbpass "db-pass"]
      (entity/create-entity ...auth... ...api... {:type       "Project"
                                                  :attributes {}}) => ...result...
      (provided
        ...auth... =contains=> {:cloudant_key dbuser :cloudant_password dbpass :cloudant_db_url dburl}
        (entity/insert-entity ...auth... {"type"       "Project"
                                          "attributes" {}}) => ...entity...
        (context/commit-transaction ...ctx...) => true
        (dao/into-seq ...api... '(...entity...)) => ...result...)))

  (fact "inserts a new entity with links inside transaction"
    (entity/create-entity ...auth... ...api... {:type       "Project"
                                                :attributes {}
                                                :links      {:my-rel [{:target_id   ...target...
                                                                       :inverse_rel ...inverse...}]}}) => ...result...
    (provided
      (util/ctx ...api...) => ...ctx...
      (context/begin-transaction ...ctx...) => true
      (entity/insert-entity ...ctx... {"type"       "Project"
                                       "attributes" {}}) => ...entity...
      (util/create-uri ...target...) => ...uri...
      (links/add-link ...entity... "my-rel" ...uri... :inverse ...inverse...) => true
      (context/commit-transaction ...ctx...) => true
      (dao/into-seq ...api... '(...entity...)) => ...result...))

  (fact "inserts a new entity with named links inside transaction"
    (entity/create-entity ...auth... ...api... {:type        "Project"
                                                :attributes  {}
                                                :named_links {:my-rel {:my-name [{:target_id   ...target...
                                                                                  :inverse_rel ...inverse...}]}}}) => ...result...
    (provided
      (util/ctx ...api...) => ...ctx...
      (context/begin-transaction ...ctx...) => true
      (entity/insert-entity ...ctx... {"type"       "Project"
                                       "attributes" {}}) => ...entity...
      (util/create-uri ...target...) => ...uri...
      (links/add-named-link ...entity... "my-rel" "my-name" ...uri... :inverse ...inverse...) => true
      (context/commit-transaction ...ctx...) => true
      (dao/into-seq ...api... '(...entity...)) => ...result...)))

;(facts "About views handlers"
;  (fact "gets view results"
;    (entity/get-view ...api... ...url...) => ...result...
;    (provided
;      (util/ctx ...api...) => ...ctx...
;      (#'ovation.entity/escape-quotes ...url...) => ...url...
;      (#'ovation.entity/get-view-results ...ctx... ...url...) => ...entities...
;      (dao/into-seq ...api... ...entities...) => ...result...))
;  (fact "get-view url-escapes \" in view query"
;    (entity/get-view ...api... "https://myserver.com/views/_foo?keys=\"123\"") => ...result...
;    (provided
;      (util/ctx ...api...) => ...ctx...
;      (entity/escape-quotes "https://myserver.com/views/_foo?keys=\"123\"") => ...url...
;      (#'ovation.entity/get-view-results ...ctx... ...url...) => ...entities...
;      (dao/into-seq ...api... ...entities...) => ...result...))
;  (fact "escape-quotes url-escapes \" in url"
;    (entity/escape-quotes "https://myserver.com/views/_foo?keys=\"123\"") =>
;    "https://myserver.com/views/_foo?keys=%22123%22"))


(facts "About entity annotations"
  (fact "`get-annotations` returns union of dto annotation documents"
    (entity/get-annotations ...api... ...id...) => (just #{{:_id   ...doc1...
                                                            :links {:self "/api/v1/entities/...id.../annotations/...doc1..."}}
                                                           {:_id   ...doc2...
                                                            :links {:self "/api/v1/entities/...id.../annotations/...doc2..."}}
                                                           {:_id   ...doc3...
                                                            :links {:self "/api/v1/entities/...id.../annotations/...doc3..."}}
                                                           {:_id   ...doc4...
                                                            :links {:self "/api/v1/entities/...id.../annotations/...doc4..."}}})
    (provided
      (dao/get-entity-annotations ...api... ...id...) => {:type  {"user1" #{{:_id ...doc1...} {:_id ...doc2...}}
                                                                  "user2" #{{:_id ...doc3...}}}
                                                          :type2 {"user1" #{{:_id ...doc4...}}}}))

  (fact "`get-specific-annotations` returns union of type annotation documents"
    (entity/get-specific-annotations ...api... ...id... :type1) => (just #{{:_id   ...doc1...
                                                                            :links {:self "/api/v1/entities/...id.../annotations/...doc1..."}}
                                                                           {:_id   ...doc2...
                                                                            :links {:self "/api/v1/entities/...id.../annotations/...doc2..."}}
                                                                           {:_id   ...doc3...
                                                                            :links {:self "/api/v1/entities/...id.../annotations/...doc3..."}}})
    (provided
      (dao/get-entity-annotations ...api... ...id...) => {:type1 {"user1" #{{:_id ...doc1...} {:_id ...doc2...}}
                                                                  "user2" #{{:_id ...doc3...}}}
                                                          :type2 {"user1" #{{:_id ...doc4...}}}})))
