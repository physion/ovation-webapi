(ns ovation.test.db.tags
  (:use midje.sweet)
  (:require [ovation.db.tags :as tags]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]))


(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `find-by-uuid`"
        (fact "should be empty"
          (let [args {:id "uuid"
                      :team_uuids [nil]
                      :organization_id 0}]
            (tags/find-by-uuid tx args) => nil)))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :organization_id 0}]
            (tags/find-all-by-uuid tx args) => ())))

)))
