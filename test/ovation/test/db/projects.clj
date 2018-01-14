(ns ovation.test.db.projects
  (:use midje.sweet)
  (:require [hugsql.core :as hugsql])
  (:require [ovation.db.projects :as projects]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..project-id..
              :organization_id ..org..
              :owner ..project-owner..
              :name ..project-name..
              :created-at ..project-created-at..
              :updated-at ..project-updated-at..
              :attributes {}
              :type c/PROJECT-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert project"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                args {:_id (str (util/make-uuid))
                      :organization_id org-id
                      :team_id team-id
                      :owner_id user-id
                      :name "A Project"
                      :attributes (util/to-json {})
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (projects/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update project"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project (factories/project tx {:organization_id org-id
                                               :team_id team-id
                                               :owner_id user-id
                                               :archived false
                                               :archived_at nil
                                               :archived_by_user_id nil
                                               :updated-at (util/iso-now)})]
            (projects/update tx project) => 1)))

      (facts "About `delete`"
        (fact "should delete project"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project (factories/project tx {:organization_id org-id
                                               :team_id team-id
                                               :owner_id user-id})
                project-count (:count (projects/count tx))]
            (projects/delete tx project)
            (:count (projects/count tx)) => (- project-count 1))))

      (facts "About `find-by-uuid`"
        (fact "should be empty"
          (let [args {:id 1
                      :team_uuids [nil]
                      :organization_id 0}]
            (projects/find-by-uuid tx args) => nil)))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (projects/find-all-by-uuid tx args) => ())))

      (facts "About `find-all`"
        (fact "should be empty"
          (let [args {:team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (projects/find-all tx args) => ())))

      (facts "About `find-all-by-rel`"
        (fact "should be empty"
          (let [args {:entity_id 1
                      :entity_type "Project"
                      :rel "Parent"
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (projects/find-all-by-rel tx args) => ())))

)))
