(ns ovation.test.core
  (:use midje.sweet)
  (:require [ovation.core :as core]
            [ovation.couch :as couch]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.auth :as auth]
            [ovation.links :as links]
            [ovation.util :as util])
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
      (tr/from-couch [...docs...]) => ...entities...
      (core/filter-trashed ...entities... false) => ...result...)))

(facts "About get-entities"
  (facts "with existing entities"
    (fact "it gets a single entity"
      (core/get-entities ...auth... [...id...]) => ...result...
      (provided
        (couch/db ...auth...) => ...db...
        (couch/all-docs ...db... [...id...]) => ...docs...
        (tr/from-couch ...docs...) => ...entities...
        (core/filter-trashed ...entities... false) => ...result...))))



(facts "About create-entity"
  (let [type ...type...
        attributes {:label ...label...}
        new-entity {:type type
                    :attributes attributes}]

    (against-background [(couch/db ...auth...) => ...db...
                         (tw/to-couch ...owner-id... [{:type       type
                                                              :attributes attributes}]
                           :collaboration_roots nil) => [...doc...]
                         (auth/authorized-user-id ...auth...) => ...owner-id...
                         (couch/bulk-docs ...db... [...doc...]) => ...result...
                         (core/parent-collaboration-roots ...auth... nil) => nil]

      (fact "it sends doc to Couch"
        (core/create-entity ...auth... [new-entity]) => ...result...)

      (facts "with parent"
        (fact "it adds collaboration roots from parent"
          (core/create-entity ...auth... [new-entity] :parent ...parent...) => ...result...
          (provided
            (core/parent-collaboration-roots ...auth... ...parent...) => ...collaboration_roots...
            (tw/to-couch ...owner-id... [{:type       type
                                                 :attributes attributes}]
              :collaboration_roots ...collaboration_roots...) => [...doc...])))

      (facts "with nil parent"
        (fact "it adds self as collaboration root"
          (core/create-entity ...auth... [new-entity] :parent nil) => ...result...)))))


(facts "About parent-collaboration-roots"
  (fact "it allows nil parent"
    (core/parent-collaboration-roots ...auth... nil) => nil)

  (fact "it returns parent links._collaboation_roots"
    (core/parent-collaboration-roots ..auth.. ..parent..) => ..roots..
    (provided
      (core/get-entities ..auth.. [..parent..]) => [{:other_stuff ..other..
                                                     :links {:_collaboration_roots ..roots..}}])))
