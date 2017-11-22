(ns ovation.test.db.notes
  (:use midje.sweet)
  (:require [ovation.db.notes :as notes]
            [ovation.test.factories :as factories]
            [ovation.test.system :as test.system]
            [clojure.java.jdbc :as jdbc]
            [ovation.constants :as c]
            [ovation.util :as util]))

(def RECORD '{:_id ..note-id..
              :organization_id ..org..
              :project ..project..
              :user ..note-user..
              :entity ..note-entity..
              :text ..text..
              :timestamp ..timestamp..
              :edited_at ..edited-at..
              :annotation_type c/NOTES
              :type c/ANNOTATION-TYPE})

(against-background [(around :contents (test.system/system-background ?form))]
  (let [db (test.system/get-db)]

    (against-background [(around :facts (jdbc/with-db-transaction [tx db]
                                        (jdbc/db-set-rollback-only! tx)
                                        ?form))]

      (facts "About `create`"
        (fact "should insert note"
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
                      :text "Some note"
                      :timestamp (util/iso-now)
                      :created-at (util/iso-now)
                      :updated-at (util/iso-now)}]
            (:generated_key (notes/create tx args)) => truthy)))

      (facts "About `update`"
        (fact "should update note"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                note (factories/note tx {:user_id user-id
                                         :organization_id org-id
                                         :project_id project-id
                                         :entity_id project-id
                                         :entity_type "Project"
                                         :edited_at (util/iso-now)})]
            (notes/update tx note) => 1)))

      (facts "About `delete`"
        (fact "should delete note"
          (let [user-id (:id (factories/user tx {}))
                org-id (:id (factories/organization tx {:owner_id user-id}))
                team-id (:id (factories/team tx {:owner_id user-id}))
                project-id (:id (factories/project tx {:organization_id org-id
                                                       :team_id team-id
                                                       :owner_id user-id}))
                note (factories/note tx {:user_id user-id
                                         :organization_id org-id
                                         :project_id project-id
                                         :entity_id project-id
                                         :entity_type "Project"})
                note-count (:count (notes/count tx))]
            (notes/delete tx note)
            (:count (notes/count tx)) => (- note-count 1))))

      (facts "About `find-by-uuid`"
        (fact "should be nil"
          (let [args {:id "uuid"
                      :team_uuids [nil]
                      :organization_id 0}]
            (notes/find-by-uuid tx args) => nil)))

      (facts "About `find-all-by-uuid`"
        (fact "should be empty"
          (let [args {:ids [nil]
                      :team_uuids [nil]
                      :organization_id 0}]
            (notes/find-all-by-uuid tx args) => ())))

)))
