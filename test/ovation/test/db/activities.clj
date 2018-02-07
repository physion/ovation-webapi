(ns ovation.test.db.activities
  (:use midje.sweet)
  (:require [ovation.db.activities :as activities]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..activity-id..
              :organization_id ..org..
              :project ..activity-project..
              :owner ..activity-owner..
              :name ..activity-name..
              :created-at ..activity-created-at..
              :updated-at ..activity-updated-at..
              :attributes {}
              :type c/ACTTIVITY-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert activity"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                args {:_id (str (util/make-uuid))
                      :organization_id org-id
                      :project_id project-id
                      :owner_id user-id
                      :name "An Activity"
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)
                      :attributes (util/to-json {})}]
            (:generated_key (activities/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update activity"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                activity (factories/activity tx {:organization_id org-id
                                                 :project_id project-id
                                                 :owner_id user-id
                                                 :archived false
                                                 :archived_at nil
                                                 :archived_by_user_id nil})]
            (activities/update tx activity) => 1)))

      (facts "About `delete`"
        (fact "should delete activity"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                activity (factories/activity tx {:organization_id org-id
                                                 :project_id project-id
                                                 :owner_id user-id})
                activity-count (:count (activities/count tx))]
            (activities/delete tx activity)
            (:count (activities/count tx)) => (- activity-count 1))))

      (facts "About `find-all`"
        (fact "should be empty"
          (let [args {:team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (activities/find-all tx args) => ())))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (activities/find-all-by-uuid tx args) => ())))

      (facts "About `find-all-by-rel`"
        (fact "should be empty"
          (let [args {:entity_id 1
                      :entity_type "Project"
                      :rel "Parent"
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (activities/find-all-by-rel tx args) => ())))

)))
