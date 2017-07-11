(ns ovation.test.storage
  (:use midje.sweet)
  (:require [ovation.storage :as st]
            [clojure.core.async :refer [chan]]
            [ovation.couch :as couch]
            [ovation.constants :as k]
            [ovation.util :refer [<??]]
            [ovation.request-context :as request-context]))

(facts "About get-organization-storage"
  (against-background [(request-context/user-id ..ctx..) => ..user..])
  (fact "Gets private org storage by project"
    (let [ch  (chan)
          org 1]
      (<?? (st/get-organization-storage ..ctx.. ..db.. org ch)) => [{:id ..p1.. :organization_id org :usage ..st1..}
                                                                    {:id ..p2.. :organization_id org :usage ..st2..}]
      (provided
        (couch/get-view ..ctx.. ..db.. k/REVISION-BYTES-VIEW
          {:startkey    [org]
           :endkey      [org {}]
           :reduce      true
           :group_level 2}
          :prefix-teams false) => [{:key [org ..p1..] :value ..st1..}
                                   {:key [org ..p2..] :value ..st2..}])))

  (fact "Gets stats for user projects in organization 0"
    (let [ch  (chan)
          org 0]
      (<?? (st/get-organization-storage ..ctx.. ..db.. org ch)) => [{:id ..p1.. :organization_id org :usage ..st1..}
                                                                    {:id ..p2.. :organization_id org :usage ..st2..}]
      (provided
        (couch/get-view ..ctx.. ..db.. k/REVISION-BYTES-VIEW
          {:startkey    [..user..]
           :endkey      [..user.. {}]
           :reduce      true
           :group_level 2}
          :prefix-teams true) => [{:rows [{:key [org ..p1..] :value ..st1..}]}
                                  {:rows [{:key [org ..p2..] :value ..st2..}]}]))))
