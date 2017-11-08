(ns ovation.test.db.timeline_events
  (:use midje.sweet)
  (:require [ovation.db.timeline_events :as timeline_events]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]))

(def RECORD {:_id "<uuid>"
             :organization_id 0
             :user "<uuid>"
             :entity "<uuid>"
             :name "name"
             :notes "notes"
             :start "0000-00-00 00:00:00"
             :end "0000-00-00 00:00:00"
             :annotation_type "TimelineEvent"
             :type "Annotation"})

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
            (timeline_events/find-by-uuid tx args) => nil)))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :organization_id 0}]
            (timeline_events/find-all-by-uuid tx args) => ())))

)))
