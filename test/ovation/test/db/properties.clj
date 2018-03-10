(ns ovation.test.db.properties
  (:use midje.sweet)
  (:require [ovation.db.properties :as properties]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..property-id..
              :organization_id ..org..
              :project ..project..
              :user ..property-user..
              :entity ..property-entity..
              :key ..property-key..
              :value ..property-value..
              :annotation_type c/PROPERTIES
              :type c/ANNOTATION-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert property"
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
                      :key "Key"
                      :value "Value"
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (properties/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update property"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                property (factories/property tx {:user_id user-id
                                                 :organization_id org-id
                                                 :project_id project-id
                                                 :entity_id project-id
                                                 :entity_type "Project"
                                                 :updated-at (util/iso-now)})]
            (properties/update tx property) => 1)))

      (facts "About `delete`"
        (fact "should delete property"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                property (factories/property tx {:user_id user-id
                                                 :organization_id org-id
                                                 :project_id project-id
                                                 :entity_id project-id
                                                 :entity_type "Project"})
                property-count (:count (properties/count tx))]
            (properties/delete tx property)
            (:count (properties/count tx)) => (- property-count 1))))

      (facts "About `find-by-uuid`"
        (fact "should be nil"
          (let [args {:id "uuid"
                      :team_uuids [nil]
                      :service_account 0
                      :organization_id 0}]
            (properties/find-by-uuid tx args) => nil)))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :service_account 0
                      :organization_id 0}]
            (properties/find-all-by-uuid tx args) => ())))

)))
