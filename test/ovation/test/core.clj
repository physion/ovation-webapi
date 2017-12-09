(ns ovation.test.core
  (:use midje.sweet)
  (:require [clojure.core.async :as async :refer [chan >!! go go-loop >! <!! <! close!]]
            [ovation.config :as config]
            [ovation.core :as core]
            [ovation.db.activities :as activities]
            [ovation.db.notes :as notes]
            [ovation.db.projects :as projects]
            [ovation.db.properties :as properties]
            [ovation.db.relations :as relations]
            [ovation.db.tags :as tags]
            [ovation.db.timeline_events :as timeline_events]
            [ovation.db.uuids :as uuids]
            [ovation.pubsub :as pubsub]
            [ovation.transform.read :as tr]
            [ovation.transform.write :as tw]
            [ovation.auth :as auth]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [slingshot.slingshot :refer [throw+]]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [ovation.constants :as c]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.links :as links]))

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (against-background [..ctx.. =contains=> {::request-context/identity ..auth..}]
        (facts "About publish-updates"
          (fact "publishes update record to publisher"
            (let [ch    (chan)
                  pchan (chan)
                  _     (async/onto-chan pchan [..result..])]
              (async/alts!! [(core/-publish-updates ..pub.. [..doc..] :channel ch)
                             (async/timeout 100)]) => [..result.. ch]
              (provided
                (pubsub/publish ..pub.. (config/config :db-updates-topic :default :updates) {:id           (str ..id..)
                                                                                             :type         ..type..
                                                                                             :organization ..org..} anything) => pchan
                ..doc.. =contains=> {:_id          ..id..
                                     :type         ..type..
                                     :organization ..org..}))))

        (facts "About values"
          (facts "read"
            (facts "`get-values`"
              (against-background [(auth/authenticated-teams ..auth..) => [..team..]
                                   (auth/authenticated-user-id ..auth..) => ..user..]
                (fact "gets values"
                  (let [args {:ids [..id..]
                              :organization_id nil
                              :team_uuids [..team..]}]
                    (core/get-values ..ctx.. ..db.. [..id..]) => [..doc1.. ..doc2.. ..doc3.. ..doc4.. ..doc5..]
                    (provided
                      (notes/find-all-by-uuid           ..db.. args) => [..doc1..]
                      (properties/find-all-by-uuid      ..db.. args) => [..doc2..]
                      (relations/find-all-by-uuid       ..db.. args) => [..doc3..]
                      (tags/find-all-by-uuid            ..db.. args) => [..doc4..]
                      (timeline_events/find-all-by-uuid ..db.. args) => [..doc5..]
                      (tr/values-from-db [..doc1..] ..ctx..) => [..doc1..]
                      (tr/values-from-db [..doc2..] ..ctx..) => [..doc2..]
                      (tr/values-from-db [..doc3..] ..ctx..) => [..doc3..]
                      (tr/values-from-db [..doc4..] ..ctx..) => [..doc4..]
                      (tr/values-from-db [..doc5..] ..ctx..) => [..doc5..]))))))

          (facts "write"
            (facts "`create-values`"
              (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
                (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\""
                  (core/create-values ..ctx.. ..db.. [{:type "Project"}]) => (throws Throwable))
                (fact "bulk-updates values"
                  (core/create-values ..ctx.. ..db.. [{:type "Annotation"}]) => [{:type "Annotation"}]
                  (provided
                    (core/-create-values-tx ..ctx.. ..db.. [{:type "Annotation"}]) => ..result..))))

            (facts "-delete-annotation-value"
              (fact "with note"
                (let [value {:annotation_type c/NOTES}]
                  (core/-delete-annotation-value ..db.. value) => value
                  (provided
                    (notes/delete ..db.. value) => 1)))
              (fact "with property"
                (let [value {:annotation_type c/PROPERTIES}]
                  (core/-delete-annotation-value ..db.. value) => value
                  (provided
                    (properties/delete ..db.. value) => 1)))
              (fact "with tag"
                (let [value {:annotation_type c/TAGS}]
                  (core/-delete-annotation-value ..db.. value) => value
                  (provided
                    (tags/delete ..db.. value) => 1)))
              (fact "with timeline event"
                (let [value {:annotation_type c/TIMELINE_EVENTS}]
                  (core/-delete-annotation-value ..db.. value) => value
                  (provided
                    (timeline_events/delete ..db.. value) => 1))))

            (facts "-delete-value"
              (fact "with relation"
                (let [value {:type c/RELATION-TYPE}]
                  (core/-delete-value ..db.. value) => value
                  (provided
                    (relations/delete ..db.. value) => 1)))
              (fact "with annotation"
                (let [value {:type c/ANNOTATION-TYPE}]
                  (core/-delete-value ..db.. value) => value
                  (provided
                    (core/-delete-annotation-value ..db.. value) => value))))

            (facts "-delete-values-tx"
              (fact "calls -delete-value with each value"
                (core/-delete-values-tx db [..value..]) => [1]
                (provided
                  (core/-delete-value anything ..value..) => 1)))

            (facts "`delete-values`"
              (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
                (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\""
                  (core/delete-values ..ctx.. ..db.. [..id..]) => (throws Throwable)
                  (provided
                    (core/get-values ..ctx.. ..db.. [..id..]) => [{:type "Project"}]))
                (fact "calls -delete-values-tx"
                  (let [value {:type "Annotation"}]
                    (core/delete-values ..ctx.. ..db.. [..id..]) => [value]
                    (provided
                      (core/get-values ..ctx.. ..db.. [..id..]) => [value]
                      (auth/check! ..ctx.. ::auth/delete) => identity
                      (core/-delete-values-tx ..db.. [value]) => [1])))))
            (facts "-update-value"
              (fact "updates relation value"
                (let [value {:type c/RELATION-TYPE}]
                  (core/-update-value ..ctx.. ..db.. value) => ..result..
                  (provided
                    (core/-ensure-organization-and-authorization ..ctx.. value ::auth/update) => value
                    (core/-update-relation-value ..ctx.. ..db.. value) => ..result..)))
              (fact "updates annotation value"
                (let [value {:type c/ANNOTATION-TYPE}]
                  (core/-update-value ..ctx.. ..db.. value) => ..result..
                  (provided
                    (core/-ensure-organization-and-authorization ..ctx.. value ::auth/update) => value
                    (core/-update-annotation-value ..ctx.. ..db.. value) => ..result..))))
            (facts "-update-values-tx"
              (fact "calls -update-value for each value"
                (core/-update-values-tx ..ctx.. db [..value..]) => [..result..]
                (provided
                  (core/-update-value ..ctx.. anything ..value..) => ..result..)))
            (facts "update-values"
              (against-background [(auth/authenticated-user-id ..auth..) => ..user..]
                (fact "throws {:type ::core/illegal-argument} if value :type not \"Annotation\" or \"Relation\""
                  (core/update-values ..ctx.. ..db.. [{:type c/PROJECT-TYPE}]) => (throws Exception))
                (fact "calls -update-values-tx"
                  (core/update-values ..ctx.. ..db.. [{:type c/ANNOTATION-TYPE}]) => [{:type c/ANNOTATION-TYPE}]
                  (provided
                    (core/-update-values-tx ..ctx.. ..db.. [{:type c/ANNOTATION-TYPE}]) => ..update..))))
            (facts "add-organization"
              (fact "adds organization_id"
                ((core/add-organization ..ctx..) {}) => {:organization_id nil}))))


        (facts "About Query"
          (facts "of-type"
            (against-background [(core/-get-entities ..ctx.. anything :include-trashed false) => ..result..]
              (fact "it gets all entities of type activity"
                (core/of-type ..ctx.. ..db.. c/ACTIVITY-TYPE) => ..result..)

              (fact "it gets all entities of type file"
                (core/of-type ..ctx.. ..db.. c/FILE-TYPE) => ..result..)

              (fact "it gets all entities of type folder"
                (core/of-type ..ctx.. ..db.. c/FOLDER-TYPE) => ..result..)

              (fact "it gets all entities of type project"
                (core/of-type ..ctx.. ..db.. c/PROJECT-TYPE) => ..result..)

              (fact "it gets all entities of type revision"
                (core/of-type ..ctx.. ..db.. c/REVISION-TYPE) => ..result..)

              (fact "it gets all entities of type source"
                (core/of-type ..ctx.. ..db.. c/SOURCE-TYPE) => ..result..)))

          (facts "get-entities"
            (facts "with existing entities"
              (fact "it gets a single entity"
                (core/get-entities ..ctx.. ..db.. [..id..]) => [..result.. ..result.. ..result.. ..result.. ..result.. ..result..]
                (provided
                  (core/-get-entities-by-id ..ctx.. anything [..id..] :include-trashed false) => [..result..]))))

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
          (facts "--create-entity"
            (let [db-fn (partial projects/create ..tx..)
                  entity {:_id (str (util/make-uuid))
                          :type c/PROJECT-TYPE}
                  record {:_id (:_id entity)
                          :organization_id ..org..
                          :owner_id ..user-id..}
                  updated-record (assoc record :id ..id..)]
              (against-background [..ctx.. =contains=> {::request-context/identity ..auth..
                                                        ::request-context/org ..org..}
                                   (auth/authenticated-user-id ..auth..) => ..user-id..]
                (fact "should call <entity>/create with transformed doc"
                  (core/--create-entity ..ctx.. ..db.. entity) => (assoc entity :id ..id..)
                  (provided
                    (tw/to-db ..ctx.. ..db.. [entity] :collaboration_roots nil
                                                      :organization_id ..org..
                                                      :user_id ..user-id..) => record
                    (projects/create ..db.. record) => {:generated_key ..id..})))))

          (facts "-create-entity"
            (let [uuid (str (util/make-uuid))
                  attributes {}
                  new-entity {:type c/SOURCE-TYPE}
                  record (assoc new-entity :id ..id..)
                  transform (fn [doc] ..result..)]
              (against-background [(uuids/create anything anything) => ..uuid-result..
                                   (uuids/update-entity anything anything) => 1
                                   (core/--create-entity ..ctx.. anything anything :collaboration_roots anything) => record
                                   (tr/db-to-entity ..ctx..) => transform
                                   (util/iso-now) => ..date..]

                (fact "creates uuid"
                  (core/-create-entity ..ctx.. db (assoc new-entity :_id uuid)) => ..result..
                  (provided
                    (uuids/create anything {:uuid uuid
                                            :entity_type (:type new-entity)
                                            :created-at ..date..
                                            :updated-at ..date..}) => ..uuid-result..))

                (fact "updates uuid"
                  (core/-create-entity ..ctx.. db (assoc new-entity :_id uuid)) => ..result..
                  (provided
                    (uuids/update-entity anything {:uuid uuid
                                                   :entity_id ..id..
                                                   :entity_type (:type new-entity)
                                                   :created-at ..date..
                                                   :updated-at ..date..}) => ..uuid-result..))

                (facts "with collaboration_roots"
                  (fact "it adds collaboration roots"
                    (core/-create-entity ..ctx.. db new-entity :collaboration_roots ..collaboration-roots..) => ..result..))

                (facts "without collaboration_roots"
                  (fact "it adds self as collaboration root"
                    (core/-create-entity ..ctx.. db new-entity :collaboration_roots nil) => ..result..))

                (facts "with Project"
                  (let [project {:type       c/PROJECT-TYPE
                                 :attributes attributes}]
                    (fact "creates team for Projects" ;; How does this test for team?!
                      (core/-create-entity ..ctx.. db project :collaboration_roots nil) => ..result..))))))

          (facts "create-entities"
            (let [type       "..type.."                           ; Anything but user
                  attributes {:label ..label..}
                  new-entity {:type       type
                              :attributes attributes}]

              (fact "it throws unauthorized exception if any :type is User"
                (core/create-entities ..ctx.. ..db.. [(assoc new-entity :type "User")]) => (throws Exception))

              (fact "it calls -create-entities-tx with entities"
                (core/create-entities ..ctx.. ..db.. [new-entity]) => ..result..
                (provided
                  (core/-create-entities-tx ..ctx.. ..db.. [new-entity] :parent nil) => ..result..))))

          (facts "update-entities"
            (let [type           "some-type"
                  attributes     {:label ..label1..}
                  new-entity     {:type       type
                                  :attributes attributes}
                  id             (util/make-uuid)
                  entity         (assoc new-entity :_id id
                                                   :owner ..owner-id..
                                                   :links {:_collaboration_roots []})
                  update         (-> entity
                                   (assoc-in [:attributes :label] ..label2..)
                                   (assoc-in [:attributes :foo] ..foo..))
                  updated-entity update]
              (against-background [(core/get-entities ..ctx.. ..db.. [id]) => [entity]]
                (fact "it updates attributes"
                  (core/update-entities ..ctx.. ..db.. [update]) => [updated-entity]
                  (provided
                    (auth/can? ..ctx.. ::auth/update anything) => true
                    (core/-update-entities-tx ..ctx.. ..db.. [updated-entity]) => [1]))
                (fact "it updates allowed keys"
                  (let [update-with-revs         (assoc update :revisions ..revs..)
                        updated-entity-with-revs (assoc updated-entity :revisions ..revs..)]
                    (core/update-entities ..ctx.. ..db.. [update-with-revs] :allow-keys [:revisions]) => [updated-entity-with-revs]
                    (provided
                      (auth/can? ..ctx.. ::auth/update anything) => true
                      (core/-update-entities-tx ..ctx.. ..db.. [updated-entity-with-revs]) => [1])))
                (fact "it updates collaboration roots"
                  (let [update2         (-> update
                                          (assoc-in [:links :_collaboration_roots] [..roots..]))
                        updated-entity2 (assoc-in updated-entity [:links :_collaboration_roots] [..roots..])]
                    (core/update-entities ..ctx.. ..db.. [update2] :update-collaboration-roots true) => [updated-entity2]
                    (provided
                      (auth/can? ..ctx.. ::auth/update anything) => true
                      (core/-update-entities-tx ..ctx.. ..db.. [update2]) => [1])))

                (fact "it fails if authenticated user doesn't have write permission"
                  (core/update-entities ..ctx.. ..db.. [update]) => (throws Exception)
                  (provided
                    (auth/can? ..ctx.. ::auth/update anything) => false))

                (fact "it throws unauthorized if entity is a User"
                  (core/update-entities ..ctx.. ..db.. [entity]) => (throws Exception)
                  (provided
                    (core/get-entities ..ctx.. ..db.. [id]) => [(assoc entity :type "User")])))))

          (facts "-restore-archived-entity"
            (let [entity {:archived true
                          :archived_at util/iso-now
                          :archived_by_user_id 1}
                  expected {:archived false
                            :archived_at nil
                            :archived_by_user_id nil}]
              (core/-restore-archived-entity entity) => expected))

          (facts "restore-deleted-entities"
            (let [id       (str (util/make-uuid))
                  entity   {:_id        id
                            :type       c/ACTIVITY-TYPE
                            :owner      ..owner..}
                  restored (core/-restore-archived-entity entity)]
              (core/restore-deleted-entities ..ctx.. ..db.. [id]) => [entity]
              (provided
                (core/get-entities ..ctx.. ..db.. [id] :include-trashed true) => [entity]
                (auth/can? ..ctx.. ::auth/update restored) => true
                (core/-update-entities-tx ..ctx.. ..db.. [restored]) => [1])))

          (facts "archive-entity"
            (fact "sets archived information"
              (let [doc  {:_id ..id..}
                    user ..user..
                    date ..date..
                    expected (-> doc
                               (assoc :archived true)
                               (assoc :archived_at ..date..)
                               (assoc :archived_by_user_id ..user..))]
                (core/archive-entity ..user.. doc) => expected
                (provided
                  (util/iso-now) => ..date..))))

          (facts "delete-entities"
            (let [type       "some-type"
                  attributes {:label ..label1..}
                  new-entity {:type       type
                              :attributes attributes}
                  id         (str (util/make-uuid))
                  entity     (assoc new-entity :_id id :owner ..owner-id..)
                  archived   (-> entity
                               (assoc :archived true)
                               (assoc :archived_at ..date..)
                               (assoc :archived_by_user_id ..owner-id..))]
              (against-background [(auth/authenticated-user-id ..auth..) => ..owner-id..]

                (against-background [(core/get-entities ..ctx.. ..db.. [id]) => [entity]
                                     (core/archive-entity ..owner-id.. entity) => archived
                                     (tw/to-db ..ctx.. ..db.. [archived]) => [archived]
                                     (core/-update-entity ..tx.. archived) => 1]
                  (fact "it trashes entity"
                    (core/delete-entities ..ctx.. ..db.. [id]) => [entity]
                    (provided
                      (auth/can? ..ctx.. ::auth/delete anything) => true
                      (core/-update-entities-tx ..ctx.. ..db.. [archived]) => [1]))

                  (fact "it fails if authenticated user doesn't have write permission"
                    (core/delete-entities ..ctx.. ..db.. [id]) => (throws Exception)
                    (provided
                      (auth/can? ..ctx.. ::auth/delete anything) => false)))

                (fact "it throws unauthorized if entity is a User"
                  (core/delete-entities ..ctx.. ..db.. [id]) => (throws Exception)
                  (provided
                    (core/get-entities ..ctx.. ..db.. [id]) => [(assoc entity :type "User")])))))

        (facts "parent-collaboration-roots"
          (fact "it allows nil parent"
            (core/parent-collaboration-roots ..ctx.. ..db.. nil) => [])

          (facts "with project"
            (let [id (str (util/make-uuid))
                  entity {:_id id
                          :type c/PROJECT-TYPE}]
              (fact "it returns :_id"
                (core/parent-collaboration-roots ..ctx.. ..db.. ..parent..) => [id]
                (provided
                  (core/get-entities ..ctx.. ..db.. [..parent..]) => [entity]))))

          (facts "with other entity"
            (let [project-id (str (util/make-uuid))
                  entity {:project_id project-id
                          :type c/FOLDER-TYPE}]
              (fact "it returns :project_id"
                (core/parent-collaboration-roots ..ctx.. ..db.. ..parent..) => [project-id]
                (provided
                  (core/get-entities ..ctx.. ..db.. [..parent..]) => [entity]))))))))))
