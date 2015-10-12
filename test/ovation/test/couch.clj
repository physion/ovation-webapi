(ns ovation.test.couch
  (:use midje.sweet)
  (:require [cemerick.url :as url]
            [ovation.couch :as couch]
            [clojure.core.async :refer [chan <!!]]
            [com.ashafa.clutch :as cl]))

(facts "About `db`"
  (fact "it constructs database URL"
    (let [dburl "https://db.db"
          username "db-user"
          password "db-pass"]

      (couch/db ...auth...) => (-> (url/url dburl)
                                 (assoc :username username :password password))
      (provided
        ...auth... =contains=> {:cloudant_key      username
                                :cloudant_password password
                                :cloudant_db_url   dburl}))))


(facts "About `get-view`"
  (fact "it returns CouchDB view result docs when include_docs=true"
    (couch/get-view "db" ...view... ...opts...) => [...result...]
    (provided
      (cl/get-view couch/design-doc ...view... ...opts...) => [{:doc ...result...}]
      ...opts... =contains=> {:include_docs true}))
  (fact "it returns CouchDB view result directly when include_docs not expclicity provided (default false)"
    (couch/get-view "db" ...view... ...opts...) => [...result...]
    (provided
      (cl/get-view couch/design-doc ...view... ...opts...) => [...result...]
      ...opts... =contains=> {}))
  (fact "it returns CouchDB view result directly when include_docs=false"
    (couch/get-view "db" ...view... ...opts...) => [...result...]
    (provided
      (cl/get-view couch/design-doc ...view... ...opts...) => [...result...]
      ...opts... =contains=> {:include_docs false})))

(facts "About `all-docs`"
  (fact "it gets docs from _all_docs"
    (couch/all-docs "dburl" ...ids...) => [...doc...]
    (provided
      (cl/all-documents {:reduce false :include_docs true} {:keys ...ids...}) => [{:doc ...doc...}])))


(facts "About `bulk-docs`"
  (fact "it POSTs bulk-update"
    (couch/bulk-docs "dburl" ...docs...) => ...result...
    (provided
      (cl/bulk-update ...docs...) => ...revs...
      (couch/merge-updates ...docs... ...revs...) => ...result...)))

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
    (let [docs [{:_id ..id1.. :_rev ..rev1..} {:_id ..id2.. :_rev ..rev2..}]
          updates [{:id ..id1.. :rev ..rev3..}]]
      (couch/merge-updates docs updates) => [{:_id ..id1.. :_rev ..rev3..} {:_id ..id2.. :_rev ..rev2..}])))

