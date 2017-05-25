(ns ovation.test.couch
  (:use midje.sweet)
  (:require [cemerick.url :as url]
            [ovation.couch :as couch]
            [clojure.core.async :refer [chan <!!]]
            [com.ashafa.clutch :as cl]
            [ovation.auth :as auth]
            [ovation.constants :as k]
            [ovation.config :as config]
            [ovation.request-context :as rc]
            [ovation.pubsub :as pubsub]))

(facts "About `db`"
  (fact "it constructs database URL"
    (let [dburl "https://db.db"
          username "db-user"
          password "db-pass"]

      (couch/db ..auth..) => (-> (url/url dburl)
                                 (assoc :username username
                                        :password password))
      (provided
        (config/config :cloudant-db-url) => dburl
        (config/config :cloudant-username) => username
        (config/config :cloudant-password) => password))))


(against-background [(rc/team-ids ..ctx..) => [..team..]
                     (rc/user-id ..ctx..) => ..user..]

  (facts "About `get-view`"
    (fact "it returns CouchDB view result docs when include_docs=true"
      (couch/get-view ..ctx.. "db" ..view.. ..opts.. :prefix-teams false) => [..result..]
      (provided
        (cl/get-view couch/API-DESIGN-DOC ..view.. ..opts..) => [{:doc ..result..}]
        ..opts.. =contains=> {:include_docs true}))

    (fact "it returns CouchDB view result docs for multi-tenant views when include_docs=true"
      (couch/get-view ..ctx.. "db" ..view.. {:startkey     [..start..]
                                             :endkey       [..end..]
                                             :include_docs true}) => [..other.. ..result..]
      (provided
        ..ctx.. =contains=> {::rc/org ..org..}
        (couch/get-view-batch ..view.. [{:startkey     [..org.. ..team.. ..start..]
                                         :endkey       [..org.. ..team.. ..end..]
                                         :include_docs true}
                                        {:startkey     [..org.. ..user.. ..start..]
                                         :endkey       [..org.. ..user.. ..end..]
                                         :include_docs true}] anything) => [..other.. ..result..]))

    (fact "it returns CouchDB view result directly when include_docs not expclicity provided (default false)"
      (couch/get-view ..auth.. "db" ..view.. ..opts.. :prefix-teams false) => [..result..]
      (provided
        (cl/get-view couch/API-DESIGN-DOC ..view.. ..opts..) => [..result..]
        ..opts.. =contains=> {}))

    (fact "it returns CouchDB view result directly when include_docs=false"
      (couch/get-view ..auth.. "db" ..view.. ..opts.. :prefix-teams false) => [..result..]
      (provided
        (cl/get-view couch/API-DESIGN-DOC ..view.. ..opts..) => [..result..]
        ..opts.. =contains=> {:include_docs false})))

  (facts "About all-docs"
    (fact "it gets docs from _all_docs"
      (couch/all-docs ..ctx.. ..db.. [..id..]) => '(..doc..)
      (provided
        (couch/get-view ..ctx.. ..db.. k/ALL-DOCS-VIEW {:keys         [..id..]
                                                        :include_docs true}) => [..doc..]
        (partition-all couch/VIEW-PARTITION [..id..]) => [[..id..]])))



  (facts "About `bulk-docs`"
    (fact "it POSTs bulk-update"
      (couch/bulk-docs "dburl" ..docs..) => ..result..
      (provided
        (cl/bulk-update ..docs..) => ..revs..
        (couch/merge-updates ..docs.. ..revs..) => ..result..))
    (fact "publishes updates"
      (couch/bulk-docs "dburl" ..docs..) => ..result..
      (provided
        (cl/bulk-update ..docs..) => ..revs..
        (couch/publish-updates ..pub.. ..revs..) => ..published..
        (couch/merge-updates ..docs.. ..revs..) => ..result..)))

  (facts "About publish-updates"
    (future-fact "publishes to :all-updates"))

  (facts "About `delete-docs`"
    (fact "it POSTs bulk-update"
      (let [doc1 {:_id  "doc1"
                  :_rev "rev1"}
            doc2 {:_id  "doc2"
                  :_rev "rev2"}]
        (couch/delete-docs "dburl" [doc1 doc2]) => ..result..
        (provided
          (couch/bulk-docs anything [(assoc doc1 :_deleted true) (assoc doc2 :_deleted true)]) => ..result..))))

  (facts "About merge-updates"
    (fact "updates _rev"
      (let [docs    [{:_id ..id1.. :_rev ..rev1..} {:_id ..id2.. :_rev ..rev2..}]
            updates [{:id ..id1.. :rev ..rev3..}]]
        (couch/merge-updates docs updates) => [{:_id ..id1.. :_rev ..rev3..} {:_id ..id2.. :_rev ..rev2..}]))))

