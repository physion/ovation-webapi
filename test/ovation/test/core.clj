(ns ovation.test.core
  (:use midje.sweet)
  (:require [ovation.core :as core]
            [ovation.couch :as couch]
            [ovation.transform :as transform])
  (:import (us.physion.ovation.data EntityDao$Views)))


(facts "About `filter-trashed"
  (facts "with include-trashed false"
    (fact "it removes documents with :trash_info"
      (core/filter-trashed [{:name ...good...}
                            {:name       ...trashed...
                             :trash_info ...info...}] false) => (seq [{:name ...good...}])))
  (facts "with include-trashed true"
    (fact "it allows documents with :trash_info"
      (core/filter-trashed [{:name ...good...}
                            {:name       ...trashed...
                             :trash_info ...info...}] true) => (seq [{:name ...good...}
                                                                     {:name       ...trashed...
                                                                      :trash_info ...info...}]))))


(facts "About `of-type`"
  (fact "it gets all entities of type"
    (core/of-type ...auth... ...type...) => ...result...
    (provided
      (couch/db ...auth...) => ...db...
      (couch/get-view ...db... EntityDao$Views/ENTITIES_BY_TYPE {:key ...type... :reduce false :include_docs true}) => [{:doc ...docs...}]
      (transform/from-couch [...docs...]) => ...entities...
      (core/filter-trashed ...entities... false) => ...result...)))

(facts "About get-entities"
  (facts "with existing entities"
    (fact "it gets a single entity"
      (core/get-entities ...auth... [...id...]) => ...result...
      (provided
        (couch/db ...auth...) => ...db...
        (couch/all-docs ...db... [...id...]) => ...docs...
        (transform/from-couch ...docs...) => ...entities...
        (core/filter-trashed ...entities... false) => ...result...))))



  (facts "About create-entity"
    (let [type ...type...
          attributes {:label ...label...}]

      (fact "it puts document"
        (core/create-entity ...auth... type attributes) => ...result...
        (provided
          (couch/db ...auth...) => ...db...
          (transform/to-couch [{:type type :attributes attributes}]) => [...doc...]
          (couch/bulk-docs ...db... [...doc...]) => ...result... ))

      (fact "it adds owner to document"
        )

      (facts "with parent"
        (future-fact "it adds collaboration roots from parent"))

      (facts "with nil parent"
        (future-fact "it adds self as collaboration root"))))
