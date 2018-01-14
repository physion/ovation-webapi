(ns ovation.test.storage
  (:use midje.sweet)
  (:require [ovation.storage :as st]
            [clojure.core.async :refer [chan]]
            [ovation.db.revisions :as revisions]
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
        (revisions/storage-by-project-for-private-org ..db..
          {:organization_id org}) => [{:id ..p1.. :organization_id org :usage ..st1..}
                                      {:id ..p2.. :organization_id org :usage ..st2..}])))

  (fact "Gets stats for user projects in organization 0"
    (let [ch  (chan)
          org 0]
      (<?? (st/get-organization-storage ..ctx.. ..db.. org ch)) => [{:id ..p1.. :organization_id org :usage ..st1..}
                                                                    {:id ..p2.. :organization_id org :usage ..st2..}]
      (provided
        (revisions/storage-by-project-for-public-org ..db..
          {:owner_id ..user..}) => [{:id ..p1.. :organization_id org :usage ..st1..}
                                    {:id ..p2.. :organization_id org :usage ..st2..}]))))
