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
            [clojure.core.async :refer [chan go >! >!! pipeline pipe]]
            [clojure.tools.logging :as logging]))

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

(def ORGANIZATION-MEMBERSHIPS "organization_memberships")

(defn read-org-tf
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

(defn read-org-collection-tf
  [ctx]
  (fn
    [response]
    (if (instance? Throwable response)
      response
      (let [orgs (:organizations response)]
        (map (read-org-tf ctx) orgs)))))

(defn read-org-single-tf
  [ctx]
  (fn [response]
    (if (instance? Throwable response)
      response
      (let [org (:organization response)]
        ((read-org-tf ctx) org)))))

(defn read-membership-tf
  [membership]
  (assoc membership :type "OrganizationMembership"))

(defn read-membership-single-tf
  [ctx]
  (fn
    [response]
    (if (instance? Throwable response)
      response
      (let [membership (:organization_membership response)]
        (read-membership-tf membership)))))

(defn read-membership-collection-tf
  [ctx]
  (fn
    [response]
    (if (instance? Throwable response)
      response
      (let [memberships (:organization_memberships response)]
        (map read-membership-tf memberships)))))



(defn get-organizations
  "Gets all organizations for authenticated user onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys a list of Organizations or Throwable."
  [ctx v1-url ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)]
    (go
      (try+
        (http/call-http raw-ch :get (make-url v1-url "organizations") (request-opts ctx) hp/ok?)
        (pipeline 1 ch (map (read-org-collection-tf ctx)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in async loop")
          (>! ch ex))))))

(defn get-organizations*
  [ctx v1-url]
  (let [ch (chan)]
    (get-organizations ctx v1-url ch)
    {:organizations (<?? ch)}))

(defn get-organization
  "Gets a single organization onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys an Organization or Throwable"
  [ctx v1-url ch org-id & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url v1-url (util/join-path ["organizations" org-id]))]
    (go
      (try+
        (http/call-http raw-ch :get url (request-opts ctx) hp/ok?)
        (pipeline 1 ch (map (read-org-single-tf ctx)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))

(defn get-organization*
  [ctx v1-url]
  (let [ch     (chan)
        org-id (::request-context/org ctx)]
    (get-organization ctx v1-url ch org-id)
    {:organization (<?? ch)}))

(defn update-organization
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
        (pipeline 1 ch (map (read-org-single-tf ctx)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))

(defn update-organization*
  [ctx v1-url body]
  (let [ch (chan)]
    (update-organization ctx v1-url ch (:organization body))
    (let [org (<?? ch)]
      {:organization org})))

(defn index-resource
  [ctx api-url rsrc collection-tf ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url api-url rsrc)
        opts   (request-opts ctx)]
    (go
      (try+
        (http/call-http raw-ch :get url opts hp/ok?)
        (pipeline 1 ch (map (collection-tf ctx)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))


(defn show-resource
  [ctx api-url rsrc id read-tf ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url api-url rsrc id)
        opts   (request-opts ctx)]
    (go
      (try+
        (http/call-http raw-ch :get url opts hp/ok?)
        (pipeline 1 ch (map (read-tf ctx)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))


(defn create-resource
  [ctx api-url rsrc body read-tf ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url api-url rsrc)
        opts   (assoc (request-opts ctx)
                 :body (util/to-json body))]
    (go
      (try+
        (http/call-http raw-ch :post url opts hp/created?)
        (pipeline 1 ch (map (read-tf ctx)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))

(defn update-resource
  [ctx api-url rsrc body read-tf id ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url api-url rsrc id)
        opts   (assoc (request-opts ctx)
                 :body (util/to-json body))]
    (go
      (try+
        (http/call-http raw-ch :put url opts hp/ok?)
        (pipeline 1 ch (map (read-tf ctx)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))

(defn destroy-resource
  [ctx api-url rsrc id read-tf ch & {:keys [close?] :or {close? true}}]
  (let [url    (make-url api-url rsrc id)
        opts   (request-opts ctx)]
    (go
      (try+
        (http/call-http ch :delete url opts (fn [response]
                                                  (or (hp/ok? response)
                                                    (hp/no-content? response))))
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))



(defn get-memberships
  [ctx api-url ch & {:keys [close?] :or {close? true}}]
  (index-resource ctx api-url ORGANIZATION-MEMBERSHIPS read-membership-collection-tf ch :close? close?))

(defn get-membership
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (show-resource ctx api-url ORGANIZATION-MEMBERSHIPS id read-membership-single-tf ch :close? close?))

(defn create-membership
  [ctx api-url body ch & {:keys [close?] :or {close? true}}]
  (create-resource ctx api-url ORGANIZATION-MEMBERSHIPS body read-membership-single-tf ch :close? close?))

(defn update-membership
  [ctx api-url id body ch & {:keys [close?] :or {close? true}}]
  (update-resource ctx api-url ORGANIZATION-MEMBERSHIPS body read-membership-single-tf id ch :close? close?))

(defn delete-membership
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (destroy-resource ctx api-url ORGANIZATION-MEMBERSHIPS id read-membership-single-tf ch :close? close?))
