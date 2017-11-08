(ns ovation.test.db.relations
  (:use midje.sweet)
  (:require [ovation.db.relations :as relations]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]))


(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

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
