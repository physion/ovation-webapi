(ns ovation.test.transform.write
  (:use midje.sweet)
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [ovation.auth :as auth]
            [ovation.constants :as c]
            [ovation.constants :as k]
            [ovation.db.projects :as projects]
            [ovation.db.uuids :as uuids]
            [ovation.request-context :as request-context]
            [ovation.routes :as r]
            [ovation.schema :as s]
            [ovation.transform.write :as tw]
            [ovation.util :as util]
            [ovation.version :refer [version]])
  (:import (clojure.lang ExceptionInfo)))


(against-background [..ctx.. =contains=> {::request-context/identity      ..auth..
                                          :ovation.request-context/routes ..rt..
                                          :ovation.request-context/org    ..org..}
                     (request-context/user-id ..ctx..) => ..owner-id..]

  (facts "ensure-organization"
    (fact "adds organization_id if not present"
      (let [doc {}]
        (tw/ensure-organization doc ..ctx..) => (assoc doc :organization_id ..org..)))
    (fact "doesn't add organization_id if present"
      (let [doc {:organization_id ..other..}]
        (tw/ensure-organization doc ..ctx..) => doc)))

  (facts "ensure-project"
    (fact "adds project_id if not present"
      (let [doc {}]
        (tw/ensure-project doc ..ctx.. ..db.. ..project..) => (assoc doc :project_id ..project-id..)
        (provided
          (projects/find-by-uuid ..db.. {:id ..project..
                                         :team_uuids [nil]
                                         :service_account 0
                                         :organization_id ..org..}) => {:id ..project-id..})))
    (fact "doesn't add project_if if present"
      (let [doc {:project_id ..other..}]
        (tw/ensure-project doc ..ctx.. ..db.. ..project..) => doc)))

  (facts "About `ensure-owner`"
    (fact "it adds owner element"
      (let [doc {:type ..type.. :attributes {:label ..label..}}]
        (tw/ensure-owner doc ..owner..) => (assoc doc :owner_id ..owner..)))
    (fact "it does not add nil owner"
      (tw/ensure-owner ..doc.. nil) => ..doc..))

  (facts "About `ensure-user`"
    (fact "it adds user_id"
      (let [doc {:type ..type.. :attributes {:label ..label..}}]
        (tw/ensure-user doc ..user-id..) => (assoc doc :user_id ..user-id..)))
    (fact "it does not add nil owner"
      (tw/ensure-user ..doc.. nil) => ..doc..))

  (facts "ensure-created-at"
    (fact "it adds created-at if not present"
      (let [doc {}]
        (tw/ensure-created-at doc ..time..) => (assoc doc :created-at ..time..)))
    (fact "it doesn't add created-at if present"
      (let [doc {:created-at ..other..}]
        (tw/ensure-created-at doc ..time..) => doc)))

  (facts "add-updated-at"
    (fact "it adds updated-at"
      (let [doc {}]
        (tw/add-updated-at doc ..time..) => (assoc doc :updated-at ..time..)))
    (fact "it replaces existing one"
      (let [doc {:updated-at ..other..}]
        (tw/add-updated-at doc ..time..) => (assoc doc :updated-at ..time..))))

  (facts "doc-to-db"
    (fact "adds organization"
      (let [doc {}]
        (tw/doc-to-db ..ctx.. ..db.. ..roots.. doc) => (contains {:organization_id ..org..})))

    (fact "adds project_id"
      (let [doc {}]
        (tw/doc-to-db ..ctx.. ..db.. [..project..] doc) => (contains {:project_id ..project-id..})
        (provided
          (projects/find-by-uuid ..db.. {:id ..project..
                                         :team_uuids [nil]
                                         :service_account 0
                                         :organization_id ..org..}) => {:id ..project-id..})))

    (fact "adds project_id from collaboration roots"
      (let [doc {:links {:_collaboration_roots [..project..]}}]
        (tw/doc-to-db ..ctx.. ..db.. nil doc) => (contains {:project_id ..project-id..})
        (provided
          (projects/find-by-uuid ..db.. {:id ..project..
                                         :team_uuids [nil]
                                         :service_account 0
                                         :organization_id ..org..}) => {:id ..project-id..})))

    (fact "adds owner"
      (let [doc {}]
        (tw/doc-to-db ..ctx.. ..db.. ..roots.. doc) => (contains {:owner_id ..owner-id..})
        (provided
          (auth/authenticated-user-id ..auth..) => ..owner-id..)))

    (fact "adds user"
      (let [doc {}]
        (tw/doc-to-db ..ctx.. ..db.. ..roots.. doc) => (contains {:user_id ..owner-id..})
        (provided
          (auth/authenticated-user-id ..auth..) => ..owner-id..)))

    (fact "adds created-at date"
      (let [time (util/iso-now)
            doc {}]
        (tw/doc-to-db ..ctx.. ..db.. ..roots.. doc) => (contains {:created-at time})
        (provided
          (util/iso-now) => time)))

    (fact "adds created-at from attributes"
      (let [time (util/iso-now)
            doc {:attributes {:created-at time}}]
        (tw/doc-to-db ..ctx.. ..db.. ..roots.. doc) => (contains {:created-at time})))

    (fact "adds updated-at date"
      (let [doc {}
            time (util/iso-now)]
        (tw/doc-to-db ..ctx.. ..db.. ..roots.. doc) => (contains {:updated-at time})
        (provided
          (util/iso-now) => time))))

  (facts "value-to-db"
    (fact "adds project_id from collaboration roots"
      (let [value {:links {:_collaboration_roots [..project..]}}]
        (tw/value-to-db ..ctx.. ..db.. value) => (contains {:project_id ..project-id..})
        (provided
          (projects/find-by-uuid ..db.. {:id ..project..
                                         :team_uuids [nil]
                                         :service_account 0
                                         :organization_id ..org..}) => {:id ..project-id..})))
    (fact "transforms entity to entity_id and entity_type"
      (let [value {:entity ..entity..}]
        (tw/value-to-db ..ctx.. ..db.. value) => (contains {:entity_id ..entity-id..
                                                            :entity_type ..entity-type..})
        (provided
          (uuids/find-by-uuid ..db.. {:uuid ..entity..}) => {:entity_id ..entity-id..
                                                             :entity_type ..entity-type..})))
    (fact "adds user"
      (let [value {}]
        (tw/value-to-db ..ctx.. ..db.. value) => (contains {:user_id ..owner-id..})
        (provided
          (auth/authenticated-user-id ..auth..) => ..owner-id..)))
    (fact "transforms annotation"
      (let [timestamp (util/iso-now)
            start (util/iso-now)
            end (util/iso-now)
            value {:annotation {:text ..text..
                                :timestamp timestamp
                                :key ..key..
                                :value ..value..
                                :tag ..tag..
                                :name ..name..
                                :notes ..notes..
                                :start start
                                :end end}}]
        (tw/value-to-db ..ctx.. ..db.. value) => (contains {:text ..text..
                                                            :timestamp timestamp
                                                            :key ..key..
                                                            :value ..value..
                                                            :tag ..tag..
                                                            :name ..name..
                                                            :notes ..notes..
                                                            :start start
                                                            :end end})))
    (fact "transforms target_id to target_id and type"
      (let [value {:target_id ..target..}]
        (tw/value-to-db ..ctx.. ..db.. value) => (contains {:target_id ..entity-id..
                                                            :target_type ..entity-type..})
        (provided
          (uuids/find-by-uuid ..db.. {:uuid ..target..}) => {:entity_id ..entity-id..
                                                     :entity_type ..entity-type..})))
     (fact "transforms source_id to source_id and type"
      (let [value {:source_id ..source..}]
        (tw/value-to-db ..ctx.. ..db.. value) => (contains {:source_id ..entity-id..
                                                            :source_type ..entity-type..})
        (provided
          (uuids/find-by-uuid ..db.. {:uuid ..source..}) => {:entity_id ..entity-id..
                                                     :entity_type ..entity-type..})))))

