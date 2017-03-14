(ns ovation.test.couch
  (:use midje.sweet)
  (:require [cemerick.url :as url]
            [ovation.couch :as couch]
            [clojure.core.async :refer [chan <!!]]
            [com.ashafa.clutch :as cl]
            [ovation.auth :as auth]
            [ovation.constants :as k]
            [ovation.config :as config]
            [ovation.request-context :as rc]))

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
        (cl/get-view couch/design-doc ..view.. ..opts..) => [{:doc ..result..}]
        ..opts.. =contains=> {:include_docs true}))

    (fact "it returns CouchDB view result docs for multi-tenant views when include_docs=true"
      (couch/get-view ..ctx.. "db" ..view.. {:startkey     [..start..]
                                             :endkey       [..end..]
                                             :include_docs true}) => [..other.. ..result..]
      (provided
        (cl/get-view couch/design-doc ..view.. {:startkey     [..user.. ..start..]
                                                :endkey       [..user.. ..end..]
                                                :include_docs true}) => [{:doc ..result..}]
        (cl/get-view couch/design-doc ..view.. {:startkey     [..team.. ..start..]
                                                :endkey       [..team.. ..end..]
                                                :include_docs true}) => [{:doc ..result..} {:doc ..other..}]))

    (fact "it returns CouchDB view result directly when include_docs not expclicity provided (default false)"
      (couch/get-view ..auth.. "db" ..view.. ..opts.. :prefix-teams false) => [..result..]
      (provided
        (cl/get-view couch/design-doc ..view.. ..opts..) => [..result..]
        ..opts.. =contains=> {}))

    (fact "it returns CouchDB view result directly when include_docs=false"
      (couch/get-view ..auth.. "db" ..view.. ..opts.. :prefix-teams false) => [..result..]
      (provided
        (cl/get-view couch/design-doc ..view.. ..opts..) => [..result..]
        ..opts.. =contains=> {:include_docs false})))

  (facts "About all-docs"
    (fact "it gets docs from _all_docs"
      (couch/all-docs ..ctx.. ..db.. ..ids..) => '(..doc..)
      (provided
        (couch/get-view ..ctx.. ..db.. k/ALL-DOCS-VIEW {:keys         ..ids..
                                                        :include_docs true}) => [..doc..]
        (partition-all couch/ALL-DOCS-PARTITION ..ids..) => [..ids..]))

    (fact "it handles 20 ids"
      (couch/all-docs ..ctx.. ..db.. [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20]) => [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20]
      (provided
        (couch/get-view ..ctx.. ..db.. k/ALL-DOCS-VIEW {:keys         '(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20)
                                                         :include_docs true}) => [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20]))

    (fact "it handles >20 ids"
      (couch/all-docs ..ctx.. ..db.. [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21]) => [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21]
      (provided
        (couch/get-view ..ctx.. ..db.. k/ALL-DOCS-VIEW {:keys         '(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20)
                                                         :include_docs true}) => [1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20]
        (couch/get-view ..ctx.. ..db.. k/ALL-DOCS-VIEW {:keys         '(21)
                                                         :include_docs true}) => [21])))



  (facts "About `bulk-docs`"
    (fact "it POSTs bulk-update"
      (couch/bulk-docs "dburl" ..docs..) => ..result..
      (provided
        (cl/bulk-update ..docs..) => ..revs..
        (couch/merge-updates ..docs.. ..revs..) => ..result..)))

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

