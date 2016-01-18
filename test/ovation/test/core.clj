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
            [ovation.constants :as k]
            [ovation.teams :as teams]))

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
          (core/create-values ..auth.. ..rt.. [{:type "Project"}]) => (throws Throwable))
        (fact "bulk-updates values"
          (core/create-values ..auth.. ..rt.. [{:type "Annotation"}]) => ..result..
          (provided
            (auth/check! ..auth.. ::auth/create) => identity
            (couch/bulk-docs ..db.. [{:type "Annotation"}]) => ..docs..
            (tr/values-from-couch ..docs.. ..auth.. ..rt..) => ..result..))))
    (facts "`delete-values`"
      (against-background [(couch/db ..auth..) => ..db..
                           (auth/authenticated-user-id ..auth..) => ..user..]
        (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\""
          (core/delete-values ..auth.. [{:type "Project"}] anything) => (throws Throwable))
        (fact "calls delete-docs"
          (core/delete-values ..auth.. [..id..] ..rt..) => ..result..
          (provided
            (couch/all-docs ..db.. [..id..]) => [{:type "Annotation"}]
            (auth/check! ..auth.. ::auth/delete) => identity
            (couch/delete-docs ..db.. [{:type "Annotation"}]) => ..docs..
            (tr/values-from-couch ..docs.. ..auth.. ..rt..) => ..result..))))))


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
        (tr/entities-from-couch [...docs...] ..auth.. ..rt..) => ...entities...
        (core/filter-trashed ...entities... false) => ...result...)))

  (facts "get-entities"
    (facts "with existing entities"
      (fact "it gets a single entity"
        (core/get-entities ...auth... [...id...] ..rt..) => ...result...
        (provided
          (couch/db ...auth...) => ...db...
          (couch/all-docs ...db... [...id...]) => [{:_id ..id1..} {:_id ..id2..}]
          (tr/entities-from-couch [{:_id ..id1..} {:_id ..id2..}] ..auth.. ..rt..) => ...entities...
          (core/filter-trashed ...entities... false) => ...result...))))

  (facts "get-owner"
    (fact "it gets the entity owner"
      (core/get-owner ..auth.. ..rt.. {:owner ..owner-id..}) => ..user..
      (provided
        (core/get-entities ..auth.. [..owner-id..] ..rt..) => [..user..]))))



(facts "About Command"
  (facts "`create-entity`"
    (let [type "..type.."                                    ; Anything but user
          attributes {:label ...label...}
          new-entity {:type       type
                      :attributes attributes}]

      (fact "it throws unauthorized exception if any :type is User"
        (core/create-entities ..auth.. [(assoc new-entity :type "User")] ..routes..) => (throws Exception)
        (provided
          (couch/db ...auth...) => ...db...))

      (against-background [(couch/db ...auth...) => ...db...
                           (tw/to-couch ...owner-id... [{:type       type
                                                         :attributes attributes}]
                             :collaboration_roots []) => [...doc...]
                           (auth/authenticated-user-id ...auth...) => ...owner-id...
                           (couch/bulk-docs ...db... [...doc...]) => ...result...
                           (tr/entities-from-couch ...result... ..auth.. ..routes..) => ...result...]

        (fact "it sends doc to Couch"
          (core/create-entities ...auth... [new-entity] ..routes..) => ...result...)

        (facts "with parent"
          (fact "it adds collaboration roots from parent"
            (core/create-entities ...auth... [new-entity] ..rt.. :parent ...parent...) => ...result...
            (provided
              (tr/entities-from-couch ...result... ..auth.. ..rt..) => ...result...
              (core/parent-collaboration-roots ...auth... ...parent... ..rt..) => ...collaboration_roots...
              (tw/to-couch ...owner-id... [{:type       type
                                            :attributes attributes}]
                :collaboration_roots ...collaboration_roots...) => [...doc...])))

        (facts "with nil parent"
          (fact "it adds self as collaboration root"
            (core/create-entities ...auth... [new-entity] ..routes.. :parent nil) => ...result...))

        (facts "with Project"
          (fact "creates team for Projects"
            (core/create-entities ..auth.. [{:type       "Project"
                                             :attributes attributes}] ..routes.. :parent nil) => [{:type "Project"
                                                                                                   :_id  ..id..}]
            (provided
              (teams/create-team {::auth/auth-info ..auth..} ..id..) => ..team..
              (tw/to-couch ...owner-id... [{:type       "Project"
                                            :attributes attributes}]
                :collaboration_roots []) => [...doc...]
              (tr/entities-from-couch ...result... ..auth.. ..routes..) => [{:type "Project"
                                                                             :_id  ..id..}]))))))

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
                   (assoc-in [:attributes :foo] ..foo..)
                   (assoc-in [:links :_collaboration_roots] [..roots..]))
          updated-entity (-> entity
                           (assoc :_rev rev2)
                           (assoc-in [:links :_collaboration_roots] [..roots..]))]
      (against-background [(couch/db ..auth..) => ..db..]
        (against-background [(tw/to-couch ..owner-id.. [new-entity] :collaboration_roots nil) => [..doc..]
                             (tw/to-couch ..owner-id.. [update]) => [update]
                             (auth/authenticated-user-id ..auth..) => ..owner-id..
                             (couch/bulk-docs ..db.. [..doc..]) => [entity]
                             (core/get-entities ..auth.. [id] ..rt..) => [entity]
                             (couch/bulk-docs ..db.. [update]) => [updated-entity]
                             (tr/entities-from-couch [updated-entity] ..auth.. ..rt..) => [updated-entity]]
          (fact "it updates attributes"
            (core/update-entities ..auth.. [update] ..rt..) => [updated-entity]
            (provided
              (auth/can? ..auth.. ::auth/update anything) => true))

          (fact "it fails if authenticated user doesn't have write permission"
            (core/update-entities ..auth.. [update] ..rt..) => (throws Exception)
            (provided
              (auth/can? ..auth.. ::auth/update anything) => false)))

        (fact "it throws unauthorized if entity is a User"
          (core/update-entities ..auth.. [entity] ..rt..) => (throws Exception)
          (provided
            (core/get-entities ..auth.. [id] ..rt..) => [(assoc entity :type "User")])))))


  (facts "`delete-entity`"
    (let [type "some-type"
          attributes {:label ..label1..}
          new-entity {:type       type
                      :attributes attributes}
          id (str (util/make-uuid))
          rev "1"
          entity (assoc new-entity :_id id :_rev rev :owner ..owner-id..)
          update (assoc entity :trash_info {:trashing_user ..owner-id...
                                            :trashing_date ..date..
                                            :trash_root    id})]
      (against-background [(couch/db ..auth..) => ..db..
                           (auth/authenticated-user-id ..auth..) => ..owner-id..]

        (against-background [(core/get-entities ..auth.. [id] ..rt..) => [entity]
                             (tw/to-couch ..owner-id.. [update]) => ..update-docs..
                             (couch/bulk-docs ..db.. ..update-docs..) => ..deleted..
                             (tr/entities-from-couch ..deleted.. ..auth.. ..rt..) => ..result..
                             (util/iso-now) => ..date..]
          (fact "it trashes entity"
            (core/delete-entity ..auth.. [id] ..rt..) => ..result..
            (provided
              (auth/can? ..auth.. ::auth/delete anything) => true))

          (fact "it fails if entity already trashed"
            (core/delete-entity ..auth.. [id] ..rt..) => (throws Exception)
            (provided
              (core/get-entities ..auth.. [id] ..rt..) => [update]))

          (fact "it fails if authenticated user doesn't have write permission"
            (core/delete-entity ..auth.. [id] ..rt..) => (throws Exception)
            (provided
              (auth/can? ..auth.. ::auth/delete anything) => false)))

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
