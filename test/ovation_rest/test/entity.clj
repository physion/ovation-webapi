(ns ovation-rest.test.entity
  (:use midje.sweet)
  (:import (us.physion.ovation.domain DtoBuilder URIs)
           (java.util UUID))
  (:require [ovation-rest.util :as util]
            [ovation-rest.interop :refer [clojurify]]
            [ovation-rest.entity :as entity]
            [ovation-rest.context :as context]
            [ovation-rest.links :as links]))



(facts "About entities"
  (fact "inserts a new entity without links"
    (entity/create-entity ...api... {:type       "Project"
                                     :attributes {}}) => ...result...
    (provided
      (util/ctx ...api...) => ...ctx...
      (context/begin-transaction ...ctx...) => true
      (entity/insert-entity ...ctx... {"type"       "Project"
                                       "attributes" {}}) => ...entity...
      (context/commit-transaction ...ctx...) => true
      (util/into-seq '(...entity...)) => ...result...))

  (fact "inserts a new entity with links inside transaction"
    (entity/create-entity ...api... {:type       "Project"
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
      (util/into-seq '(...entity...)) => ...result...))

  (fact "inserts a new entity with named links inside transaction"
    (entity/create-entity ...api... {:type        "Project"
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
      (util/into-seq '(...entity...)) => ...result...))

  (fact "updates entity attributes"
    (entity/update-entity-attributes ...api... ...id... {:attr1 1
                                                         :attr2 "value"}) => ...result...
    (provided
      (util/get-entity ...api... ...id...) => ...entity...
      (util/entity-to-dto ...entity...) => {:_id        "123"
                                            :_rev       "rev1"
                                            :attributes {:attr1 0
                                                         :attr3 "foo"}
                                            :links      {:self "/entity/uri/"}}

      (#'ovation-rest.entity/update-entity ...entity... {:_id        "123"
                                                                 :_rev       "rev1"
                                                                 :attributes {:attr1 1
                                                                              :attr2 "value"}
                                                                 :links      {:self "/entity/uri/"}}) => ...entity...
      (util/into-seq '(...entity...)) => ...result...)))

(facts "About top-level handlers"
  (fact "gets projects"
    (entity/index-resource ...apikey... "projects") => ...result...
    (provided
      (util/ctx ...apikey...) => ...ctx...
      (#'ovation-rest.entity/get-projects ...ctx...) => ...entities...
      (util/into-seq ...entities...) => ...result...))

  (fact "gets top-level sources"
    (entity/index-resource ...apikey... "sources") => ...result...
    (provided
      (util/ctx ...apikey...) => ...ctx...
      (#'ovation-rest.entity/get-sources ...ctx...) => ...entities...
      (util/into-seq ...entities...) => ...result...))

  (fact "gets protocols"
    (entity/index-resource ...apikey... "protocols") => ...result...
    (provided
      (util/ctx ...apikey...) => ...ctx...
      (#'ovation-rest.entity/get-protocols ...ctx...) => ...entities...
      (util/into-seq ...entities...) => ...result...)))

(facts "About views handlers"
  (fact "gets view results"
    (entity/get-view ...api... ...url...) => ...result...
    (provided
      (util/ctx ...api...) => ...ctx...
      (#'ovation-rest.entity/escape-quotes ...url...) => ...url...
      (#'ovation-rest.entity/get-view-results ...ctx... ...url...) => ...entities...
      (util/into-seq ...entities...) => ...result...))
  (fact "get-view url-escapes \" in view query"
    (entity/get-view ...api... "https://myserver.com/views/_foo?keys=\"123\"") => ...result...
    (provided
      (util/ctx ...api...) => ...ctx...
      (entity/escape-quotes "https://myserver.com/views/_foo?keys=\"123\"") => ...url...
      (#'ovation-rest.entity/get-view-results ...ctx... ...url...) => ...entities...
      (util/into-seq ...entities...) => ...result...))
  (fact "escape-quotes url-escapes \" in url"
    (entity/escape-quotes "https://myserver.com/views/_foo?keys=\"123\"") =>
    "https://myserver.com/views/_foo?keys=%22123%22"))