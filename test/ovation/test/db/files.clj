(ns ovation.test.db.files
  (:use midje.sweet)
  (:require [ovation.db.files :as files]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..file-id..
              :organization_id ..org..
              :project ..file-project..
              :owner ..file-owner..
              :name ..file-name..
              :created-at ..file-created-at..
              :updated-at ..file-updated-at..
              :attributes {}
              :type c/FILE-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert file"
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
                      :name "A File"
                      :attributes (util/to-json {})
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (files/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update file"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                file (factories/file tx {:organization_id org-id
                                         :project_id project-id
                                         :owner_id user-id
                                         :archived false
                                         :archived_at nil
                                         :archived_by_user_id nil})]
            (files/update tx file) => 1)))

      (facts "About `delete`"
        (fact "should delete note"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                file (factories/file tx {:organization_id org-id
                                         :project_id project-id
                                         :owner_id user-id})
                file-count (:count (files/count tx))]
            (files/delete tx file)
            (:count (files/count tx)) => (- file-count 1))))

      (facts "About `find-all`"
        (fact "should be empty"
          (let [args {:team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (files/find-all tx args) => ())))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (files/find-all-by-uuid tx args) => ())))

      (facts "About `find-all-by-rel`"
        (fact "should be empty"
          (let [args {:entity_id 1
                      :entity_type "Project"
                      :rel "Parent"
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (files/find-all-by-rel tx args) => ())))

)))
