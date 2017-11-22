(ns ovation.test.db.tags
  (:use midje.sweet)
  (:require [ovation.db.tags :as tags]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..tag-id..
              :organization_id ..org..
              :project ..project..
              :user ..tag-user..
              :entity ..tag-entity..
              :tag ..tag..
              :annotation_type c/TAGS
              :type c/ANNOTATION-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert tag"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                args {:_id (str (util/make-uuid))
                      :user_id user-id
                      :organization_id org-id
                      :project_id project-id
                      :entity_id project-id
                      :entity_type "Project"
                      :tag "Tag"
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (tags/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update tag"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                tag (factories/tag tx {:user_id user-id
                                       :organization_id org-id
                                       :project_id project-id
                                       :entity_id project-id
                                       :entity_type "Project"})]
            (tags/update tx tag) => 1)))

      (facts "About `delete`"
        (fact "should delete tag"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                tag (factories/tag tx {:user_id user-id
                                       :organization_id org-id
                                       :project_id project-id
                                       :entity_id project-id
                                       :entity_type "Project"})
                tag-count (:count (tags/count tx))]
            (tags/delete tx tag)
            (:count (tags/count tx)) => (- tag-count 1))))

      (facts "About `find-by-uuid`"
        (fact "should be empty"
          (let [args {:id "uuid"
                      :team_uuids [nil]
                      :organization_id 0}]
            (tags/find-by-uuid tx args) => nil)))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :organization_id 0}]
            (tags/find-all-by-uuid tx args) => ())))

)))
