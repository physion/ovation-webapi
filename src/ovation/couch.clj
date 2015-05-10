(ns ovation.couch
  (:require [cemerick.url :as url]
            [com.ashafa.clutch :as cl]
            [clojure.core.async :refer [>!!]]))

(def design-doc "dao")                                      ;; Design doc defined by Java API

(defn db
  "Database URL from authorization info"
  [auth]
  (-> (url/url (:cloudant_db_url auth))
    (assoc :username (:cloudant_key auth)
           :password (:cloudant_password auth))))

(defn get-view
  [db view opts]
  (cl/with-db db
    (cl/get-view design-doc view opts)))

(defn all-docs
  "Gets all documents with given document IDs"
  [db ids]
  (cl/with-db db
    (cl/all-documents {:reduce false :include_docs true} {:keys ids})))

(defn bulk-docs
  "Creates or updates documents"
  [db docs]
  (cl/with-db db
    (cl/bulk-update docs)))

(defn changes
  "Writes changes to channel c.

  Options:
    :continuous [true|false]
    :since <db-seq>"
  [db c & {:as opts}]
  (let [changes-agent (cl/change-agent db opts)]
    (add-watch changes-agent :update (fn [key ref old new] (>!! c new)))
    (cl/start-changes changes-agent)))