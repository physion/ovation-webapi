(ns ovation.test.db.revisions
  (:use midje.sweet)
  (:require [ovation.db.revisions :as revisions]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..revision-id..
              :organization_id ..org..
              :project ..revision-project..
              :owner ..revision-owner..
              :name ..revision-name..
              :content_type ..revision-content-type..
              :content_length ..revision-content-length..
              :upload_status ..revision-upload-status..
              :url ..revision-url..
              :created-at ..revision-created-at..
              :updated-at ..revision-updated-at..
              :attributes {}
              :file_id ..revision-file-id..
              :type c/REVISION-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert revision"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                file-id (:id (factories/file tx {:organization_id org-id
                                                 :project_id project-id
                                                 :owner_id user-id}))
                args {:_id (str (util/make-uuid))
                      :organization_id org-id
                      :owner_id user-id
                      :file_id file-id
                      :resource_id nil
                      :name "A Revision"
                      :version nil
                      :content_type "text/plain"
                      :content_length nil
                      :upload_status nil
                      :url nil
                      :attributes (util/to-json {})
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (revisions/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update revision"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                file-id (:id (factories/file tx {:organization_id org-id
                                                 :project_id project-id
                                                 :owner_id user-id}))
                revision (factories/revision tx {:organization_id org-id
                                                 :owner_id user-id
                                                 :file_id file-id
                                                 :archived false
                                                 :archived_at nil
                                                 :archived_by_user_id nil
                                                 :updated-at (util/iso-now)})]
            (revisions/update tx revision) => 1)))

      (facts "About `delete`"
        (fact "should delete revision"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                file-id (:id (factories/file tx {:organization_id org-id
                                                 :project_id project-id
                                                 :owner_id user-id}))
                revision (factories/revision tx {:organization_id org-id
                                                 :owner_id user-id
                                                 :file_id file-id})
                revision-count (:count (revisions/count tx))]
            (revisions/delete tx revision)
            (:count (revisions/count tx)) => (- revision-count 1))))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (revisions/find-all-by-uuid tx args) => ())))

      (facts "About `find-all-by-rel`"
        (fact "should be empty"
          (let [args {:entity_id 1
                      :entity_type "Project"
                      :rel "Parent"
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (revisions/find-all-by-rel tx args) => ())))

      (facts "About `storage-by-project-for-public-org`"
        (fact "should be empty"
          (let [args {:owner_id 1
                      :organization_id 0}]
            (revisions/storage-by-project-for-public-org tx args) => ())))

      (facts "About `storage-by-project-for-private-org`"
        (fact "should be empty"
          (let [args {:owner_id 1
                      :organization_id 1}]
            (revisions/storage-by-project-for-private-org tx args) => ())))

      (facts "About `find-head-by-file-id`"
        (fact "should be empty"
          (let [args {:organization_id 0
                      :team_uuids [nil]
                      :file_id "<uuid>"}]
            (revisions/find-head-by-file-id tx args) => ())))

)))
