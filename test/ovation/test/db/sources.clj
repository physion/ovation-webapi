(ns ovation.test.db.sources
  (:use midje.sweet)
  (:require [ovation.db.sources :as sources]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD {:_id "<uuid>"
             :organization_id 0
             :owner "<uuid>"
             :name "name"
             :created-at "0000-00-00 00:00:00"
             :updated-at "0000-00-00 00:00:00"
             :attributes {}
             :type c/SOURCE-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `find-all`"
        (fact "should be empty"
          (let [args {:owner_id 1
                      :team_uuids [nil]
                      :archived 0
                      :organization_id 0}]
            (sources/find-all tx args) => ())))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
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
                      :owner_id 1
                      :archived 0
                      :organization_id 0}]
            (sources/find-all-by-rel tx args) => ())))

)))
