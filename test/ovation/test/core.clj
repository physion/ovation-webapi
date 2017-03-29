(ns ovation.test.core
  (:use midje.sweet)
  (:require [ovation.core :as core]
            [ovation.couch :as couch]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.auth :as auth]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [slingshot.slingshot :refer [throw+]]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [ovation.constants :as k]
            [ovation.links :as links]))

(against-background [..ctx.. =contains=> {::request-context/auth ..auth..}]
  (facts "About values"
    (facts "read"
      (facts "`get-values`"
        (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
          (fact "gets values"
            (core/get-values ..ctx.. ..db.. [..id..]) => [..doc..]
            (provided
              (couch/all-docs ..ctx.. ..db.. [..id..]) => [..doc..])))))
    (facts "write"
      (facts "`create-values`"
        (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
          (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\""
            (core/create-values ..ctx.. ..db.. [{:type "Project"}]) => (throws Throwable))
          (fact "bulk-updates values"
            (core/create-values ..ctx.. ..db.. [{:type "Annotation"}]) => ..result..
            (provided
              (auth/check! ..auth.. ::auth/create) => identity
              (couch/bulk-docs ..db.. [{:type "Annotation"}]) => ..docs..
              (tr/values-from-couch ..docs.. ..ctx..) => ..result..))))
      (facts "`delete-values`"
        (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
          (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\""
            (core/delete-values ..ctx.. ..db.. [..id..]) => (throws Throwable)
            (provided
              (couch/all-docs ..ctx.. ..db.. [..id..]) => [{:type "Project" :_id ..id..}]))
          (fact "calls delete-docs"
            (core/delete-values ..ctx.. ..db.. [..id..]) => ..result..
            (provided
              (couch/all-docs ..ctx.. ..db.. [..id..]) => [{:type "Annotation"}]
              (auth/check! ..auth.. ::auth/delete) => identity
              (couch/delete-docs ..db.. [{:type "Annotation"}]) => ..docs..
              (tr/values-from-couch ..docs.. ..ctx..) => ..result..))))
      (facts "update-values"
        (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
          (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\" or \"Relation\""
            (core/update-values ..ctx.. ..db.. [{:type "Project"}]) => (throws Exception))
          (fact "bulk-updates values"
            (core/update-values ..ctx.. ..db.. [{:type "Annotation"}]) => ..result..
            (provided
              (auth/check! ..auth.. ::auth/update) => identity
              (couch/bulk-docs ..db.. [{:type "Annotation"}]) => ..docs..
              (tr/values-from-couch ..docs.. ..ctx..) => ..result..))))))


  (facts "About Query"
    (facts "`filter-trashed"
      (facts "with include-trashed false"
        (fact "it removes documents with :trash_info"
          (core/filter-trashed [{:name ..good..}
                                {:name       ..trashed..
                                 :trash_info ..info..}] false) => (seq [{:name ..good..}])))
      (facts "with include-trashed true"
        (fact "it allows documents with :trash_info"
          (core/filter-trashed [{:name ..good..}
                                {:name       ..trashed..
                                 :trash_info ..info..}] true) => (seq [{:name ..good..}
                                                                       {:name       ..trashed..
                                                                        :trash_info ..info..}]))))


    (facts "`of-type`"
      (fact "it gets all entities of type"
        (core/of-type ..ctx.. ..db.. ..type..) => ..result..
        (provided
          (couch/get-view ..ctx.. ..db.. k/ENTITIES-BY-TYPE-VIEW {:key ..type.. :reduce false :include_docs true}) => [..docs..]
          (tr/entities-from-couch [..docs..] ..ctx..) => ..entities..
          (core/filter-trashed ..entities.. false) => ..result..)))

    (facts "get-entities"
      (facts "with existing entities"
        (fact "it gets a single entity"
          (core/get-entities ..ctx.. ..db.. [..id..]) => ..result..
          (provided
            (couch/all-docs ..ctx.. ..db.. [..id..]) => [{:_id ..id1..} {:_id ..id2..}]
            (tr/entities-from-couch [{:_id ..id1..} {:_id ..id2..}] ..ctx..) => ..entities..
            (core/filter-trashed ..entities.. false) => ..result..))))

    (facts "get-owner"
      (fact "it gets the entity owner"
        (core/get-owner ..ctx.. ..db.. {:owner ..owner-id..}) => ..user..
        (provided
          (core/get-entities ..ctx.. ..db.. [..owner-id..]) => [..user..])))

  (facts "get-entity"
    (fact "calls get-entities"
      (core/get-entity ..ctx.. ..db.. ..id..) => ..result..
      (provided
        (core/get-entities ..ctx.. ..db.. [..id..] :include-trashed false) => [..result..])))

  (facts "get-owner"
    (fact "it gets the entity owner"
      (core/get-owner ..ctx.. ..db.. {:owner ..owner-id..}) => ..user..
      (provided
        (core/get-entities ..ctx.. ..db.. [..owner-id..]) => [..user..]))))


  (facts "About Command"
    (facts "create-entity"
      (let [type       "..type.."                           ; Anything but user
            attributes {:label ..label..}
            new-entity {:type       type
                        :attributes attributes}]

        (fact "it throws unauthorized exception if any :type is User"
          (core/create-entities ..ctx.. ..db.. [(assoc new-entity :type "User")]) => (throws Exception))

        (against-background [(tw/to-couch ..ctx.. [{:type       type
                                                         :attributes attributes}]
                               :collaboration_roots []) => [..doc..]
                             (auth/authenticated-user-id ..auth..) => ..owner-id..
                             (couch/bulk-docs ..db.. [..doc..]) => ..result..
                             (tr/entities-from-couch ..result.. ..ctx..) => ..result..]


          (fact "it sends doc to Couch"
            (core/create-entities ..ctx.. ..db.. [new-entity]) => ..result..)

          (facts "with parent"
            (fact "it adds collaboration roots from parent"
              (core/create-entities ..ctx.. ..db.. [new-entity] :parent ..parent..) => ..result..
              (provided
                (tr/entities-from-couch ..result.. ..ctx..) => ..result..
                (core/parent-collaboration-roots ..ctx.. ..db.. ..parent..) => ..collaboration_roots..
                (tw/to-couch ..ctx.. [{:type       type
                                            :attributes attributes}]
                  :collaboration_roots ..collaboration_roots..) => [..doc..])))

          (facts "with nil parent"
            (fact "it adds self as collaboration root"
              (core/create-entities ..ctx.. ..db.. [new-entity] :parent nil) => ..result..))

          (facts "with Project"
            (fact "creates team for Projects"
              (core/create-entities ..ctx.. ..db.. [{:type       "Project"
                                                     :attributes attributes}] :parent nil) => [{:type "Project"
                                                                                                :_id  ..id..}]
              (provided
                (tw/to-couch ..ctx.. [{:type       "Project"
                                            :attributes attributes}]
                  :collaboration_roots []) => [..doc..]
                (tr/entities-from-couch ..result.. ..ctx..) => [{:type "Project"
                                                                 :_id  ..id..}]))))))

    (facts "update-entity"
      (let [type           "some-type"
            attributes     {:label ..label1..}
            new-entity     {:type       type
                            :attributes attributes}
            id             (util/make-uuid)
            rev            "1"
            rev2           "2"
            entity         (assoc new-entity :_id id
                                             :_rev rev
                                             :owner ..owner-id..
                                             :links {:_collaboration_roots []})
            update         (-> entity
                             (assoc-in [:attributes :label] ..label2..)
                             (assoc-in [:attributes :foo] ..foo..))
            updated-entity (-> entity
                             (assoc :_rev rev2))]
        (against-background [(tw/to-couch ..ctx.. [new-entity] :collaboration_roots nil) => [..doc..]
                             (tw/to-couch ..ctx.. [update]) => [update]
                             (auth/authenticated-user-id ..auth..) => ..owner-id..
                             (couch/bulk-docs ..db.. [..doc..]) => [entity]
                             (core/get-entities ..ctx.. ..db.. [id]) => [entity]
                             (couch/bulk-docs ..db.. [update]) => [updated-entity]
                             (tr/entities-from-couch [updated-entity] ..ctx..) => [updated-entity]]
          (fact "it updates attributes"
            (core/update-entities ..ctx.. ..db.. [update]) => [updated-entity]
            (provided
              (auth/can? ..auth.. ::auth/update anything) => true))
          (fact "it updates allowed keys"
            (let [update-with-revs         (assoc update :revisions ..revs..)
                  updated-entity-with-revs (assoc updated-entity :revisions ..revs..)]
              (core/update-entities ..ctx.. ..db.. [update-with-revs] :allow-keys [:revisions]) => [updated-entity-with-revs]
              (provided
                (auth/can? ..auth.. ::auth/update anything) => true
                (couch/bulk-docs ..db.. [update-with-revs]) => [updated-entity-with-revs]
                (tw/to-couch ..ctx.. [update-with-revs]) => [update-with-revs]
                (tr/entities-from-couch [updated-entity-with-revs] ..ctx..) => [updated-entity-with-revs])))
          (fact "it updates collaboration roots"
            (let [update2         (-> update
                                    (assoc-in [:links :_collaboration_roots] [..roots..]))
                  updated-entity2 (assoc-in updated-entity [:links :_collaboration_roots] [..roots..])]
              (core/update-entities ..ctx.. ..db.. [update2] :update-collaboration-roots true) => [updated-entity2]
              (provided
                (auth/can? ..auth.. ::auth/update anything) => true
                (tw/to-couch ..ctx.. [update2]) => [update2]
                (couch/bulk-docs ..db.. [update2]) => [updated-entity2]
                (tr/entities-from-couch [updated-entity2] ..ctx..) => [updated-entity2])))

          (fact "it fails if authenticated user doesn't have write permission"
            (core/update-entities ..ctx.. ..db.. [update]) => (throws Exception)
            (provided
              (auth/can? ..auth.. ::auth/update anything) => false))

          (fact "it throws unauthorized if entity is a User"
            (core/update-entities ..ctx.. ..db.. [entity]) => (throws Exception)
            (provided
              (core/get-entities ..ctx.. ..db.. [id]) => [(assoc entity :type "User")])))))


    (facts "restore-deleted-entities"
      (let [id       (str (util/make-uuid))
            rev      "1"
            entity   {:_id        id
                      :type       "some-type"
                      :rev        rev
                      :owner      ..owner..
                      :attributes {:my "attributes"}
                      :trash_info {:trashing_user ..owner-id..
                                   :trashing_date ..date..
                                   :trash_root    id}}
            restored (core/restore-trashed-entity ..auth.. entity)]
        (core/restore-deleted-entities ..ctx.. ..db.. [id]) => ..result..
        (provided
          (auth/authenticated-user-id ..auth..) => ..owner..
          (core/get-entities ..ctx.. ..db.. [id] :include-trashed true) => [entity]
          (tw/to-couch ..ctx.. [restored]) => ..couch-docs..
          (couch/bulk-docs ..db.. ..couch-docs..) => ..restored..
          (tr/entities-from-couch ..restored.. ..ctx..) => ..result..
          (auth/can? ..auth.. ::auth/update restored) => true)))

    (facts "delete-entity"
      (let [type       "some-type"
            attributes {:label ..label1..}
            new-entity {:type       type
                        :attributes attributes}
            id         (str (util/make-uuid))
            rev        "1"
            entity     (assoc new-entity :_id id :_rev rev :owner ..owner-id..)
            update     (assoc entity :trash_info {:trashing_user ..owner-id..
                                                  :trashing_date ..date..
                                                  :trash_root    id})]
        (against-background [(auth/authenticated-user-id ..auth..) => ..owner-id..]

          (against-background [(core/get-entities ..ctx.. ..db.. [id]) => [entity]
                               (tw/to-couch ..ctx.. [update]) => ..update-docs..
                               (couch/bulk-docs ..db.. ..update-docs..) => ..deleted..
                               (tr/entities-from-couch ..deleted.. ..ctx..) => ..result..
                               (util/iso-now) => ..date..]
            (fact "it trashes entity"
              (core/delete-entities ..ctx.. ..db.. [id]) => ..result..
              (provided
                (auth/can? ..auth.. ::auth/delete anything) => true))

            (fact "it fails if entity already trashed"
              (core/delete-entities ..ctx.. ..db.. [id]) => (throws Exception)
              (provided
                (core/get-entities ..ctx.. ..db.. [id]) => [update]))

            (fact "it fails if authenticated user doesn't have write permission"
              (core/delete-entities ..ctx.. ..db.. [id]) => (throws Exception)
              (provided
                (auth/can? ..auth.. ::auth/delete anything) => false)))

          (fact "it throws unauthorized if entity is a User"
            (core/delete-entities ..ctx.. ..db.. [id]) => (throws Exception)
            (provided
              (core/get-entities ..ctx.. ..db.. [id]) => [(assoc entity :type "User")])))))

    (facts "trash-entity helper"
      (fact "adds required info"
        (let [doc  {:_id ..id..}
              info {:trashing_user ..user..
                    :trashing_date ..date..
                    :trash_root    ..id..}]
          (:trash_info (core/trash-entity ..user.. doc)) => info
          (provided
            (t/now) => ..dt..
            (tf/unparse (tf/formatters :date-hour-minute-second-ms) ..dt..) => ..date..)))))

  (facts "About restore-trashed-entity helper"
    (fact "removes :trash_info"
      (let [doc {:_id        ..id..
                 :attributes {}
                 :trash_info ..trash..}]
        (core/restore-trashed-entity ..auth.. doc) => (dissoc doc :trash_info)))

    (facts "parent-collaboration-roots"
      (fact "it allows nil parent"
        (core/parent-collaboration-roots ..ctx.. ..db.... nil) => [])

      (fact "it returns parent links._collaboation_roots"
        (core/parent-collaboration-roots ..ctx.. ..db.. ..parent..) => ..roots..
        (provided
          (core/get-entities ..ctx.. ..db.. [..parent..]) => [{:other_stuff ..other..
                                                               :links       {:_collaboration_roots ..roots..}}])))))
