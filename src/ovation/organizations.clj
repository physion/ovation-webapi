(ns ovation.organizations
  (:require [ovation.routes :as routes]
            [ovation.http :as http]
            [ovation.util :as util :refer [<??]]
            [ovation.config :as config]
            [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity!]]
            [ovation.request-context :as request-context]
            [slingshot.support :refer [get-throwable]]
            [ring.util.http-predicates :as hp]
            [slingshot.slingshot :refer [try+]]
            [clojure.core.async :refer [chan go >! >!! pipeline]]))

(defn request-opts
  [ctx]
  {:timeout     10000                                       ; ms
   :oauth-token (request-context/token ctx)
   :headers     {"Content-Type" "application/json; charset=utf-8"
                 "Accept"       "application/json"}})

(defn make-url
  [base & comps]
  (util/join-path (conj comps base)))

(defn make-links
  [ctx org]
  (let [rt (::request-context/routes ctx)]
    {:self                     (routes/self-route ctx "organization" (:id org))
     :projects                 (routes/org-projects-route rt (:id org))
     :organization-memberships (routes/org-memberships-route rt (:id org))
     :organization-groups      (routes/org-groups-route rt (:id org))}))

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

(defn read-single-tf
  [ctx]
  (fn [response]
    (if (instance? Throwable response)
      response
      (let [org (:organization response)]
        ((read-tf ctx) org)))))



(defn get-organizations-async
  "Gets all organizations for authenticated user onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys a list of Organizations or Throwable."
  [ctx v1-url ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)]
    (go
      (try+
        (http/call-http raw-ch :get (make-url v1-url "organizations") (request-opts ctx) hp/ok?)
        (pipeline 1 ch (map (read-collection-tf ctx)) raw-ch close?)
        (catch Object ex
          (>! ch ex))))))

(defn get-organizations*
  [ctx v1-url]
  (let [ch (chan)]
    (get-organizations-async ctx v1-url ch)
    {:organizations (<?? ch)}))

(defn get-organization-async
  "Gets a single organization onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys an Organization or Throwable"
  [ctx v1-url ch org-id & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url v1-url (util/join-path ["organizations" org-id]))]
    (go
      (try+
        (http/call-http raw-ch :get url (request-opts ctx) hp/ok?)
        (pipeline 1 ch (map (read-single-tf ctx)) raw-ch close?)
        (catch Object ex
          (>! ch ex))))))

(defn get-organization*
  [ctx v1-url]
  (let [ch     (chan)
        org-id (::request-context/org ctx)]
    (get-organization-async ctx v1-url ch org-id)
    {:organization (<?? ch)}))

(defn update-organization-async
  "Updates a single organization, returning the result on the provided channel.
   Only org name is updated.

   If close?, channel is closed on completion (default true).

   Conveys an Organization or Throwable"
  [ctx v1-url ch org & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        {org-id :id
         name   :name} org
        url    (make-url v1-url (util/join-path ["organizations" org-id]))
        opts   (assoc (request-opts ctx)
                 :body (util/to-json {:organization {:id   org-id
                                                     :name name}}))]
    (go
      (try+
        (http/call-http raw-ch :put url opts hp/ok?)
        (pipeline 1 ch (map (read-single-tf ctx)) raw-ch close?)
        (catch Object ex
          (>! ch ex))))))

(defn update-organization*
  [ctx v1-url body]
  (let [ch (chan)]
    (update-organization-async ctx v1-url ch (:organization body))
    (let [org (<?? ch)]
      {:organization org})))

(defn get-memberships
  [ctx ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)]
    (go
      ;;TODO /api/v2/organization-memberships
      (http/call-http raw-ch :get (make-url "organization-memberships") (request-opts ctx) hp/ok?)
      (pipeline 1 ch (map (read-collection-tf ctx)) raw-ch close?))))
