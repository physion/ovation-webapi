(ns ovation.test.db.properties
  (:use midje.sweet)
  (:require [ovation.db.properties :as properties]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD {:_id "<uuid>"
             :organization_id 0
             :user "<uuid>"
             :entity "<uuid>"
             :key "key"
             :value "value"
             :annotation_type c/PROPERTIES
             :type c/ANNOTATION-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `find-by-uuid`"
        (fact "should be nil"
          (let [args {:id "uuid"
                      :team_uuids [nil]
                      :organization_id 0}]
            (properties/find-by-uuid tx args) => nil)))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :organization_id 0}]
            (properties/find-all-by-uuid tx args) => ())))

)))
