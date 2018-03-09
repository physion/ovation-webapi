(ns ovation.test.db.timeline_events
  (:use midje.sweet)
  (:require [ovation.db.timeline_events :as timeline_events]
            [ovation.test.factories :as factories]
            [ovation.util :as util]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]))

(def RECORD '{:_id ..timeline-event-id..
              :organization_id ..org..
              :project_id ..project..
              :user ..timeline-event-user..
              :entity ..timeline-event-entity..
              :name ..timeline-event-name..
              :notes ..timeline-event-notes..
              :start ..timeline-event-start..
              :end ..timeline-event-end..
              :annotation_type c/TIMELINE_EVENTS
              :type c/ANNOTATION-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert timeline_event"
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
                      :name "Timeline event"
                      :notes "A note"
                      :start (util/iso-now)
                      :end (util/iso-now)
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (timeline_events/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update timeline_event"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                timeline_event (factories/timeline_event tx {:user_id user-id
                                                             :organization_id org-id
                                                             :project_id project-id
                                                             :entity_id project-id
                                                             :entity_type "Project"})]
            (timeline_events/update tx timeline_event) => 1)))

      (facts "About `delete`"
        (fact "should delete timeline_event"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                timeline_event (factories/timeline_event tx {:user_id user-id
                                                             :organization_id org-id
                                                             :project_id project-id
                                                             :entity_id project-id
                                                             :entity_type "Project"})
                timeline_event-count (:count (timeline_events/count tx))]
            (timeline_events/delete tx timeline_event)
            (:count (timeline_events/count tx)) => (- timeline_event-count 1))))

      (facts "About `find-by-uuid`"
        (fact "should be empty"
          (let [args {:id "uuid"
                      :team_uuids [nil]
                      :service_account 0
                      :organization_id 0}]
            (timeline_events/find-by-uuid tx args) => nil)))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :service_account 0
                      :organization_id 0}]
            (timeline_events/find-all-by-uuid tx args) => ())))

)))
