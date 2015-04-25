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
  (fact "it returns CouchDB view result"
    (couch/get-view "db" ...view... ...opts...) => ...result...
    (provided
      (cl/get-view couch/design-doc ...view... ...opts...) => ...result... )))

(facts "About `all-docs`"
  (fact "it gets docs from _all_docs"
    (couch/all-docs "dburl" ...ids...) => ...docs...
    (provided
      (cl/all-documents {:reduce false :include_docs true} {:keys ...ids...}) => ...docs...)))


(facts "About `bulk-docs`"
  (fact "it POSTs bulk-update"
    (couch/bulk-docs "dburl" ...docs...) => ...result...
    (provided
      (cl/bulk-update ...docs...) => ...result...)))

