(ns ovation.test.couch
  (:use midje.sweet)
  (:require [cemerick.url :as url]
            [ovation.couch :as couch]
            [com.ashafa.clutch :as cl]))

(facts "About database"
       (fact "`db` constructs database URL"
             (let [dburl "https://db.db"
                   username "db-user"
                   password "db-pass"]

               (couch/db ...auth...) => (-> (url/url dburl)
                                          (assoc :username username :password password))
               (provided
                 ...auth... =contains=> {:cloudant_key      username
                                         :cloudant_password password
                                         :cloudant_db_url   dburl}))))
