(ns ovation.test.db.folders
  (:use midje.sweet)
  (:require [ovation.db.folders :as folders]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..folder-id..
              :organization_id ..org..
              :project ..folder-project..
              :owner ..folder-owner..
              :name ..folder-name..
              :created-at ..folder-created-at..
              :updated-at ..folder-updated-at..
              :attributes {}
              :type c/FOLDER-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert folder"
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
                      :name "A Folder"
                      :attributes (util/to-json {})
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (folders/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update folder"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                folder (factories/folder tx {:organization_id org-id
                                             :project_id project-id
                                             :owner_id user-id
                                             :archived false
                                             :archived_at nil
                                             :archived_by_user_id nil
                                             :updated-at (util/iso-now)})]
            (folders/update tx folder) => 1)))

      (facts "About `delete`"
        (fact "should delete folder"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                folder (factories/folder tx {:organization_id org-id
                                             :project_id project-id
                                             :owner_id user-id})
                folder-count (:count (folders/count tx))]
            (folders/delete tx folder)
            (:count (folders/count tx)) => (- folder-count 1))))

      (facts "About `find-all`"
        (fact "should be empty"
          (let [args {:team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (folders/find-all tx args) => ())))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :owner_id 1
                      :archived 0
                      :organization_id 0}]
            (folders/find-all-by-uuid tx args) => ())))

      (facts "About `find-all-by-rel`"
        (fact "should be empty"
          (let [args {:entity_id 1
                      :entity_type "Project"
                      :rel "Parent"
                      :team_uuids [nil]
                      :owner_id 1
                      :archived 0
                      :organization_id 0}]
            (folders/find-all-by-rel tx args) => ())))

)))
