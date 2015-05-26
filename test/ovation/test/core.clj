(ns ovation.test.core
  (:use midje.sweet)
  (:require [ovation.core :as core]
            [ovation.couch :as couch]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.auth :as auth]
            [ovation.util :as util]
            [slingshot.slingshot :refer [throw+]]
            [clj-time.core :as t]
            [clj-time.format :as tf])
  (:import (us.physion.ovation.data EntityDao$Views)))


(facts "About Query"
  (facts "`filter-trashed"
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


  (facts "`of-type`"
    (fact "it gets all entities of type"
      (core/of-type ...auth... ...type...) => ...result...
      (provided
        (couch/db ...auth...) => ...db...
        (couch/get-view ...db... EntityDao$Views/ENTITIES_BY_TYPE {:key ...type... :reduce false :include_docs true}) => [{:doc ...docs...}]
        (tr/from-couch [...docs...]) => ...entities...
        (core/filter-trashed ...entities... false) => ...result...)))

  (facts "get-entities"
    (facts "with existing entities"
      (fact "it gets a single entity"
        (core/get-entities ...auth... [...id...]) => ...result...
        (provided
          (couch/db ...auth...) => ...db...
          (couch/all-docs ...db... [...id...]) => ...docs...
          (tr/from-couch ...docs...) => ...entities...
          (core/filter-trashed ...entities... false) => ...result...)))))



(facts "About Command"
  (facts "`create-entity`"
    (let [type ...type...
          attributes {:label ...label...}
          new-entity {:type       type
                      :attributes attributes}]

      (against-background [(couch/db ...auth...) => ...db...
                           (tw/to-couch ...owner-id... [{:type       type
                                                         :attributes attributes}]
                             :collaboration_roots []) => [...doc...]
                           (auth/authorized-user-id ...auth...) => ...owner-id...
                           (couch/bulk-docs ...db... [...doc...]) => ...result...]

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

  (facts "`update-entity`"
    (let [type "some-type"
          attributes {:label ..label1..}
          new-entity {:type       type
                      :attributes attributes}
          id (util/make-uuid)
          rev "1"
          rev2 "2"
          entity (assoc new-entity :_id id :_rev rev :owner ..owner-id..)
          update (-> entity
                   (assoc-in [:attributes :label] ..label2..)
                   (assoc-in [:attributes :foo] ..foo..))
          updated-entity (assoc entity :_rev rev2)]
      (against-background [(couch/db ..auth..) => ..db..
                           (tw/to-couch ..owner-id.. [new-entity] :collaboration_roots nil) => [..doc..]
                           (auth/authorized-user-id ..auth..) => ..owner-id..
                           (couch/bulk-docs ..db.. [..doc..]) => [entity]
                           (core/get-entities ..auth.. [id]) => [entity]
                           (couch/bulk-docs ..db.. [update]) => [updated-entity]
                           ]

        (fact "it updates attributes"
          (core/update-entity ..auth.. [update]) => [updated-entity])
        (fact "it fails if authenticated user doesn't have write permission"
          (core/update-entity ..auth.. [update]) => (throws Exception)
          (provided
            (auth/authorized-user-id ..auth..) => ..other-id..
            (auth/can? ..other-id.. :auth/update) => false)))))


  (facts "`delete-entity`"
    (let [type "some-type"
          attributes {:label ..label1..}
          new-entity {:type       type
                      :attributes attributes}
          id (util/make-uuid)
          rev "1"
          entity (assoc new-entity :_id id :_rev rev :owner ..owner-id..)
          update (assoc entity :trash_info {:trashing_user ..owner-id...
                                            :trashing_date ..date..
                                            :trash_root id})]
      (against-background [(couch/db ..auth..) => ..db..
                           (auth/authorized-user-id ..auth..) => ..owner-id..
                           (core/get-entities ..auth.. [id]) => [entity]
                           (couch/bulk-docs ..db.. [update]) => ..deleted..
                           (util/iso-now) => ..date..
                           ]

        (fact "it trashes entity"
          (core/delete-entity ..auth.. [id]) => ..deleted..)

        (fact "it fails if entity already trashed"
          (core/delete-entity ..auth.. [id]) => (throws Exception)
          (provided
            (core/get-entities ..auth.. [id]) => [update]))

        (fact "it fails if authenticated user doesn't have write permission"
          (core/delete-entity ..auth.. [id]) => (throws Exception)
          (provided
            (auth/authorized-user-id ..auth..) => ..other-id..
            (auth/can? ..other-id.. :auth/delete) => (fn [doc] (throw+ {:boom doc})))))))

  (facts "`trash-entity` helper"
    (fact "adds required info"
      (let [doc {:_id ..id..}
            info {:trashing_user ..user..
                  :trashing_date ..date..
                  :trash_root    ..id..}]
        (:trash_info (core/trash-entity ..user.. doc)) => info
        (provided
          (t/now) => ..dt..
          (tf/unparse (tf/formatters :date) ..dt..) => ..date..))))

  (facts "`parent-collaboration-roots`"
    (fact "it allows nil parent"
      (core/parent-collaboration-roots ...auth... nil) => [])

    (fact "it returns parent links._collaboation_roots"
      (core/parent-collaboration-roots ..auth.. ..parent..) => ..roots..
      (provided
        (core/get-entities ..auth.. [..parent..]) => [{:other_stuff ..other..
                                                       :links       {:_collaboration_roots ..roots..}}]))))
