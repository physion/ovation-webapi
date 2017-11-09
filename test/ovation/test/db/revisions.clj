(ns ovation.test.db.revisions
  (:use midje.sweet)
  (:require [ovation.db.revisions :as revisions]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD {:_id "<uuid>"
             :organization_id 0
             :project "<uuid>"
             :owner "<uuid>"
             :name "name"
             :content_type "text/plain"
             :content_length 100
             :upload_status "Complete"
             :url "https://"
             :created-at "0000-00-00 00:00:00"
             :updated-at "0000-00-00 00:00:00"
             :attributes {}
             :file_id "<uuid>"
             :type c/REVISION-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

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

)))
