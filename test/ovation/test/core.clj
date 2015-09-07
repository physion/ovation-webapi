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
            [clj-time.format :as tf]
            [ovation.constants :as k]))

(facts "About values"
  (facts "read"
    (facts "`get-values`"
      (against-background [(couch/db ..auth..) => ..db..
                           (auth/authenticated-user-id ..auth..) => ..user..]
        (fact "gets values"
          (core/get-values ..auth.. [..id..]) => [..doc..]
          (provided
            (couch/all-docs ..db.. [..id..]) => [..doc..])))))
  (facts "write"
    (facts "`create-values`"
      (against-background [(couch/db ..auth..) => ..db..
                           (auth/authenticated-user-id ..auth..) => ..user..]
        (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\""
          (core/create-values ..auth.. [{:type "Project"}]) => (throws Throwable))
        (fact "bulk-updates values"
          (core/create-values ..auth.. [{:type "Annotation"}]) => ..result..
          (provided
            (auth/check! ..user.. ::auth/create) => identity
            (couch/bulk-docs ..db.. [{:type "Annotation"}]) => ..result..))))
    (facts "`delete-values`"
      (against-background [(couch/db ..auth..) => ..db..
                           (auth/authenticated-user-id ..auth..) => ..user..]
        (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\""
          (core/delete-values ..auth.. [{:type "Project"}] anything) => (throws Throwable))
        (fact "calls delete-docs"
          (core/delete-values ..auth.. [..id..] anything) => ..result..
          (provided
            (couch/all-docs ..db.. [..id..]) => [{:type "Annotation"}]
            (auth/check! ..user.. ::auth/delete) => identity
            (couch/delete-docs ..db.. [{:type "Annotation"}]) => ..result..))))))


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
      (core/of-type ...auth... ...type... ..rt..) => ...result...
      (provided
        (couch/db ...auth...) => ...db...
        (couch/get-view ...db... k/ENTITIES-BY-TYPE-VIEW {:key ...type... :reduce false :include_docs true}) => [...docs...]
        (tr/entities-from-couch [...docs...] ..rt..) => ...entities...
        (core/filter-trashed ...entities... false) => ...result...)))

  (facts "get-entities"
    (facts "with existing entities"
      (fact "it gets a single entity"
        (core/get-entities ...auth... [...id...] ..rt..) => ...result...
        (provided
          (couch/db ...auth...) => ...db...
          (couch/all-docs ...db... [...id...]) => [{:_id ..id1..} {:_id ..id2..}]
          (tr/entities-from-couch [{:_id ..id1..} {:_id ..id2..}] ..rt..) => ...entities...
          (core/filter-trashed ...entities... false) => ...result...)))))



(facts "About Command"
  (facts "`create-entity`"
    (let [type ...type...
          attributes {:label ...label...}
          new-entity {:type       type
                      :attributes attributes}]

      (fact "it throws unauthorized exception if any :type is User"
        (core/create-entity ..auth.. [(assoc new-entity :type "User")] ..routes..) => (throws Exception)
        (provided
          (couch/db ...auth...) => ...db...))

      (against-background [(couch/db ...auth...) => ...db...
                           (tw/to-couch ...owner-id... [{:type       type
                                                         :attributes attributes}]
                             :collaboration_roots []) => [...doc...]
                           (auth/authenticated-user-id ...auth...) => ...owner-id...
                           (couch/bulk-docs ...db... [...doc...]) => ...result...]

        (fact "it sends doc to Couch"
          (core/create-entity ...auth... [new-entity] ..routes..) => ...result...)

        (facts "with parent"
          (fact "it adds collaboration roots from parent"
            (core/create-entity ...auth... [new-entity] ..rt.. :parent ...parent...) => ...result...
            (provided
              (core/parent-collaboration-roots ...auth... ...parent... ..rt..) => ...collaboration_roots...
              (tw/to-couch ...owner-id... [{:type       type
                                            :attributes attributes}]
                :collaboration_roots ...collaboration_roots...) => [...doc...])))

        (facts "with nil parent"
          (fact "it adds self as collaboration root"
            (core/create-entity ...auth... [new-entity] ..routes.. :parent nil) => ...result...)))))

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
      (against-background [(couch/db ..auth..) => ..db..]
        (against-background [(tw/to-couch ..owner-id.. [new-entity] :collaboration_roots nil) => [..doc..]
                             (tw/to-couch ..owner-id.. [update]) => [update]
                             (auth/authenticated-user-id ..auth..) => ..owner-id..
                             (couch/bulk-docs ..db.. [..doc..]) => [entity]
                             (core/get-entities ..auth.. [id] ..rt..) => [entity]
                             (couch/bulk-docs ..db.. [update]) => [updated-entity]
                             (tr/entities-from-couch [updated-entity] ..rt..) => [updated-entity]]
          (fact "it updates attributes"
            (core/update-entity ..auth.. [update] ..rt..) => [updated-entity])
          (fact "it fails if authenticated user doesn't have write permission"
            (core/update-entity ..auth.. [update] ..rt..) => (throws Exception)
            (provided
              (auth/authenticated-user-id ..auth..) => ..other-id..
              (auth/can? ..other-id.. :auth/update anything) => false)))

        (fact "it throws unauthorized if entity is a User"
          (core/update-entity ..auth.. [entity] ..rt..) => (throws Exception)
          (provided
            (core/get-entities ..auth.. [id] ..rt..) => [(assoc entity :type "User")])))))


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
                                            :trash_root    id})]
      (against-background [(couch/db ..auth..) => ..db..
                           (auth/authenticated-user-id ..auth..) => ..owner-id..]

        (against-background [(core/get-entities ..auth.. [id] ..rt..) => [entity]
                             (couch/bulk-docs ..db.. [update]) => ..deleted..
                             (util/iso-now) => ..date..]
          (fact "it trashes entity"
            (core/delete-entity ..auth.. [id] ..rt..) => ..deleted..)

          (fact "it fails if entity already trashed"
            (core/delete-entity ..auth.. [id] ..rt..) => (throws Exception)
            (provided
              (core/get-entities ..auth.. [id] ..rt..) => [update]))

          (fact "it fails if authenticated user doesn't have write permission"
            (core/delete-entity ..auth.. [id] ..rt..) => (throws Exception)
            (provided
              (auth/authenticated-user-id ..auth..) => ..other-id..
              (auth/can? ..other-id.. :auth/delete anything) => false)))

        (fact "it throws unauthorized if entity is a User"
          (core/delete-entity ..auth.. [id] ..rt..) => (throws Exception)
          (provided
            (core/get-entities ..auth.. [id] ..rt..) => [(assoc entity :type "User")])))))

  (facts "`trash-entity` helper"
    (fact "adds required info"
      (let [doc {:_id ..id..}
            info {:trashing_user ..user..
                  :trashing_date ..date..
                  :trash_root    ..id..}]
        (:trash_info (core/trash-entity ..user.. doc)) => info
        (provided
          (t/now) => ..dt..
          (tf/unparse (tf/formatters :date-hour-minute-second-ms) ..dt..) => ..date..))))

  (facts "`parent-collaboration-roots`"
    (fact "it allows nil parent"
      (core/parent-collaboration-roots ...auth... nil ..rt..) => [])

    (fact "it returns parent links._collaboation_roots"
      (core/parent-collaboration-roots ..auth.. ..parent.. ..rt..) => ..roots..
      (provided
        (core/get-entities ..auth.. [..parent..] ..rt..) => [{:other_stuff ..other..
                                                              :links       {:_collaboration_roots ..roots..}}]))))
