(ns ovation.organizations
  (:require [ovation.routes :as routes]
            [ovation.http :as http]
            [ovation.util :as util :refer [<??]]
            [ovation.config :as config]
            [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity!]]
            [ovation.request-context :as request-context]
            [slingshot.support :refer [get-throwable]]
            [ring.util.http-predicates :as hp]
            [clojure.core.async :refer [chan go >! >!! pipeline]]))

(defn request-opts
  [ctx]
  {:timeout     10000                                       ; ms
   :oauth-token (request-context/token ctx)
   :headers     {"Content-Type" "application/json; charset=utf-8"}})

(defn make-url
  [& comps]
  (util/join-path (conj comps config/ORGS_SERVER)))

(defn make-links
  [ctx org]
  (let [rt (::request-context/routes ctx)]
    {:self     (routes/self-route ctx "organization" (:id org))
     :projects (routes/org-projects-route rt (:id org))}))

(defn read-tf
  [ctx]
  (fn
    [org]
    (if (instance? Throwable org)
      org
      {:id    (:id org)
       :type  "Organization"
       :uuid  (:uuid org)
       :name  (:name org)
       :links (make-links ctx org)})))

(defn read-collection-tf
  [ctx]
  (fn
    [response]
    (if (instance? Throwable response)
      response
      (let [orgs (:organizations response)]
        (map (read-tf ctx) orgs)))))



(defn get-organizations
  "Gets all organizations for authenticated user onto provided channel.
  If close?, channel is closed on completion (default true).
   Conveys a list of Organizations."
  [ctx ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)]
    (go
      (http/call-http raw-ch :get (make-url "organizations") (request-opts ctx) hp/ok?)
      (pipeline 1 ch (map (read-collection-tf ctx)) raw-ch close?))))

(defn get-organizations*
  [ctx]
  (let [ch (chan)]
    (get-organizations ctx ch)
    {:organizations (<?? ch)}))
