(ns ovation.couch
  (:require [cemerick.url :as url]
            [com.ashafa.clutch :as cl]
            [ovation.annotations :as annotations]
            [ovation.dao :as dao]))

(def design-doc "dao")                                      ;; Design doc defined by Java API

(defn db
  "Database URL from authorization info"
  [auth]
  (-> (url/url (:cloudant_db_url auth))
    (assoc :username (:cloudant_key auth)
           :password (:cloudant_password auth))))


(defn transform
  "Transform couchdb documents. This needs a better name!"
  [docs]
  (map (fn [doc] (->> doc
                   (dao/remove-private-links)
                   (dao/links-to-rel-path)
                   (annotations/add-annotation-links)       ;; NB must come after links-to-rel-path
                   ;;TODO add-self-link
                   ))
    docs))
