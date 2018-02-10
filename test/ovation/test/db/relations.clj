(ns ovation.test.db.relations
  (:use midje.sweet)
  (:require [ovation.db.relations :as relations]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..relation-id..
              :organization_id ..org..
              :project ..relation-project..
              :rel ..relation-rel..
              :inverse_rel ..relation-inverse-rel..
              :source_id ..relation-source-id..
              :target_id ..relation-target-id..
              :user_id ..relation-user-id..
              :type c/RELATION-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert relation"
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
                      :rel "Parent"
                      :inverse_rel "Child"
                      :source_id project-id
                      :source_type "Project"
                      :target_id project-id
                      :target_type "Project"}]
            (:generated_key (relations/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update relation"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                relation (factories/relation tx {:user_id user-id
                                                 :organization_id org-id
                                                 :project_id project-id
                                                 :source_id project-id
                                                 :source_type "Project"
                                                 :target_id project-id
                                                 :target_type "Project"})]
            (relations/update tx relation) => 1)))

      (facts "About `delete`"
        (fact "should delete relation"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                relation (factories/relation tx {:user_id user-id
                                                 :organization_id org-id
                                                 :project_id project-id
                                                 :source_id project-id
                                                 :source_type "Project"
                                                 :target_id project-id
                                                 :target_type "Project"})
                relation-count (:count (relations/count tx))]
            (relations/delete tx relation)
            (:count (relations/count tx)) => (- relation-count 1))))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :organization_id 0}]
            (relations/find-all-by-uuid tx args) => ())))

      (facts "About `find-all-by-parent-entity-rel`"
        (fact "should be empty"
          (let [args {:entity_id 1
                      :entity_type "Project"
                      :rel "Parent"}]
            (relations/find-all-by-parent-entity-rel tx args) => ())))

)))
