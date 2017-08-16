(ns ovation.test.couch
  (:use midje.sweet)
  (:require [ovation.couch :as couch]
            [clojure.core.async :refer [chan <!! >!!] :as async]
            [com.ashafa.clutch :as cl]
            [ovation.util :refer [<??]]
            [ovation.constants :as k]
            [ovation.request-context :as request-context]
            [ovation.pubsub :as pubsub]
            [ovation.util :as util]
            [ovation.auth :as auth]))


(against-background [(request-context/team-ids ..ctx..) => [..team..]
                     (request-context/user-id ..ctx..) => ..user..
                     ..db.. =contains=> {:connection ..db..}]

  (facts "About `get-view`"
    (fact "it adds _service when caller is a service and prefix-teams is false"
      (couch/get-view ..ctx.. "db" "view_name" ..opts.. :prefix-teams false) => [..result..]
      (provided
        (cl/get-view couch/API-DESIGN-DOC "view_name_service" ..opts..) => [{:doc ..result..}]
        ..opts.. =contains=> {:include_docs true}
        ..ctx.. =contains=> {::request-context/identity ..auth..
                             ::request-context/org      ..org..}
        (auth/service-account? ..auth..) => true))

    (fact "it adds _service when caller is a service and prefix-teams is true"
      (couch/get-view ..ctx.. "db" "view_name" ..opts.. :prefix-teams true) => [..result..]
      (provided
        ..ctx.. =contains=> {::request-context/identity ..auth..
                             ::request-context/org      ..org..}
        (auth/service-account? ..auth..) => true
        (cl/get-view couch/API-DESIGN-DOC "view_name_service" ..opts..) => [{:doc ..result..}]
        ..opts.. =contains=> {:include_docs true}
        (couch/prefix-keys ..opts.. ..org..) => ..opts..))

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
        ..ctx.. =contains=> {::request-context/org ..org..}
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
        ..opts.. =contains=> {:include_docs false}))))

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
  (fact "it publishes updates"
    (couch/bulk-docs ..db.. ..docs..) => ..result..
    (provided
      ..db.. =contains=> {:connection "db-url"
                          :pubsub     {:publisher ..pub..}}
      (cl/bulk-update ..docs..) => ..revs..
      (couch/publish-updates ..pub.. ..revs.. :channel anything) => ..published..
      (couch/merge-updates ..docs.. ..revs..) => ..result..)))

(facts "About publish-updates"
  (fact "publishes update record to publisher"
    (let [ch    (chan)
          pchan (chan)
          _     (async/onto-chan pchan [..result..])]
      (async/alts!! [(couch/publish-updates ..pub.. [..doc..] :channel ch)
                     (async/timeout 100)]) => [..result.. ch]
      (provided
        (pubsub/publish ..pub.. :updates {:id   ..id..
                                          :rev  ..rev..
                                          :type ..type..} anything) => pchan
        ..doc.. =contains=> {:_id  ..id..
                             :_rev ..rev..
                             :type ..type..}))))

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
    (let [doc1-id (util/make-uuid)
          doc2-id (str (util/make-uuid))
          docs    [{:_id doc1-id :_rev ..rev1..} {:_id doc2-id :_rev ..rev2..} {:_id ..doc3.. :_rev ..rev5..}]
          updates [{:id (str doc1-id) :rev ..rev3..} {:id (str doc2-id) :rev ..rev4..}]]
      (couch/merge-updates docs updates) => [{:_id doc1-id :_rev ..rev3..} {:_id doc2-id :_rev ..rev4..} {:_id ..doc3.. :_rev ..rev5..}])))

