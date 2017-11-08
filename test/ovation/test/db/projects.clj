(ns ovation.test.db.projects
  (:use midje.sweet)
  (:require [ovation.db.projects :as projects]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]))


(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

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
