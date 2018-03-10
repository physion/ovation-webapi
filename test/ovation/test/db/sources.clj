(ns ovation.test.db.sources
  (:use midje.sweet)
  (:require [ovation.db.sources :as sources]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..source-id..
              :organization_id ..org..
              :owner ..source-owner..
              :name ..source-name..
              :created-at ..source-created-at..
              :updated-at ..source-updated-at..
              :attributes {}
              :type c/SOURCE-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert source"
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
                      :name "A Source"
                      :attributes (util/to-json {})
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (sources/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update source"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                source (factories/source tx {:organization_id org-id
                                             :project_id project-id
                                             :owner_id user-id
                                             :archived false
                                             :archived_at nil
                                             :archived_by_user_id nil
                                             :updated-at (util/iso-now)})]
            (sources/update tx source) => 1)))

      (facts "About `delete`"
        (fact "should delete source"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                source (factories/source tx {:organization_id org-id
                                             :project_id project-id
                                             :owner_id user-id})
                source-count (:count (sources/count tx))]
            (sources/delete tx source)
            (:count (sources/count tx)) => (- source-count 1))))

      (facts "About `find-all`"
        (fact "should be empty"
          (let [args {:owner_id 1
                      :team_uuids [nil]
                      :service_account 0
                      :archived 0
                      :organization_id 0}]
            (sources/find-all tx args) => ())))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :service_account 0
                      :owner_id 1
                      :archived 0
                      :organization_id 0}]
            (sources/find-all-by-uuid tx args) => ())))

      (facts "About `find-all-by-rel`"
        (fact "should be empty"
          (let [args {:entity_id 1
                      :entity_type "Project"
                      :rel "Parent"
                      :team_uuids [nil]
                      :service_account 0
                      :owner_id 1
                      :archived 0
                      :organization_id 0}]
            (sources/find-all-by-rel tx args) => ())))

)))
