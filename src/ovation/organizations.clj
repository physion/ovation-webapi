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


(def ORGANIZATION-MEMBERSHIPS "organization_memberships")
(def ORGANIZATION-GROUPS "organization_groups")


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


(defn make-read-org-tf
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

(defn make-read-membership-tf
  [ctx]
  (fn
    [membership]
    (assoc membership :type "OrganizationMembership")))



(defn read-collection-tf
  [ctx key make-tf]
  (fn
    [response]
    (if (instance? Throwable response)
      response
      (let [orgs (key response)]
        (map (make-tf ctx) orgs)))))

(defn read-single-tf
  [ctx key make-tf]
  (fn [response]
    (if (instance? Throwable response)
      response
      (let [org (key response)]
        ((make-tf ctx) org)))))



(defn get-organizations
  "Gets all organizations for authenticated user onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys a list of Organizations or Throwable."
  [ctx api-url ch & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)]
    (go
      (try+
        (http/call-http raw-ch :get (make-url api-url "organizations") (request-opts ctx) hp/ok?)
        (pipeline 1 ch (map (read-collection-tf ctx :organizations make-read-org-tf)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in async loop")
          (>! ch ex))))))

(defn get-organizations*
  [ctx api-url]
  (let [ch (chan)]
    (get-organizations ctx api-url ch)
    {:organizations (<?? ch)}))

(defn get-organization
  "Gets a single organization onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys an Organization or Throwable"
  [ctx api-url ch org-id & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url api-url (util/join-path ["organizations" org-id]))]
    (go
      (try+
        (http/call-http raw-ch :get url (request-opts ctx) hp/ok?)
        (pipeline 1 ch (map (read-single-tf ctx :organization make-read-org-tf)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))

(defn get-organization*
  [ctx api-url]
  (let [ch     (chan)
        org-id (::request-context/org ctx)]
    (get-organization ctx api-url ch org-id)
    {:organization (<?? ch)}))

(defn update-organization
  "Updates a single organization, returning the result on the provided channel.
   Only org name is updated.

   If close?, channel is closed on completion (default true).

   Conveys an Organization or Throwable"
  [ctx api-url ch org & {:keys [close?] :or {close? true}}]
  (let [raw-ch (chan)
        {org-id :id
         name   :name} org
        url    (make-url api-url (util/join-path ["organizations" org-id]))
        opts   (assoc (request-opts ctx)
                 :body (util/to-json {:organization {:id   org-id
                                                     :name name}}))]
    (go
      (try+
        (http/call-http raw-ch :put url opts hp/ok?)
        (pipeline 1 ch (map (read-single-tf ctx :organization make-read-org-tf)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))

(defn update-organization*
  [ctx api-url body]
  (let [ch (chan)]
    (update-organization ctx api-url ch (:organization body))
    (let [org (<?? ch)]
      {:organization org})))

(defn index-resource
  [ctx api-url rsrc ch & {:keys [close? response-key make-tf query-params] :or {close?       true
                                                                                query-params nil}}]
  (let [raw-ch (chan)
        url    (make-url api-url rsrc)
        opts   (assoc (request-opts ctx)
                 :query-params query-params)]
    (go
      (try+
        (http/call-http raw-ch :get url opts hp/ok?)
        (pipeline 1 ch (map (read-collection-tf ctx response-key make-tf)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))


(defn show-resource
  [ctx api-url rsrc id ch & {:keys [close? response-key make-tf query-params] :or {close?       true
                                                                                   query-params nil}}]
  (let [raw-ch (chan)
        url    (make-url api-url rsrc id)
        opts   (request-opts ctx)]
    (go
      (try+
        (http/call-http raw-ch :get url opts hp/ok?)
        (pipeline 1 ch (map (read-single-tf ctx response-key make-tf)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))


(defn create-resource
  [ctx api-url rsrc body ch & {:keys [close? response-key make-tf] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url api-url rsrc)
        opts   (assoc (request-opts ctx)
                 :body (util/to-json body))]
    (go
      (try+
        (http/call-http raw-ch :post url opts hp/created?)
        (pipeline 1 ch (map (read-single-tf ctx response-key make-tf)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))

(defn update-resource
  [ctx api-url rsrc body id ch & {:keys [close? response-key make-tf] :or {close? true}}]
  (let [raw-ch (chan)
        url    (make-url api-url rsrc id)
        opts   (assoc (request-opts ctx)
                 :body (util/to-json body))]
    (go
      (try+
        (http/call-http raw-ch :put url opts hp/ok?)
        (pipeline 1 ch (map (read-single-tf ctx response-key make-tf)) raw-ch close?)
        (catch Object ex
          (logging/error ex "Exception in go block")
          (>! ch ex))))))

(defn destroy-resource
  [ctx api-url rsrc id ch & {:keys [close? response-key make-tf] :or {close? true}}]
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
  (index-resource ctx api-url ORGANIZATION-MEMBERSHIPS ch
    :close? close?
    :response-key :organization_memberships
    :make-tf make-read-membership-tf))

(defn get-membership
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (show-resource ctx api-url ORGANIZATION-MEMBERSHIPS id ch
    :close? close?
    :response-key :organization_membership
    :make-tf make-read-membership-tf))

(defn create-membership
  [ctx api-url body ch & {:keys [close?] :or {close? true}}]
  (create-resource ctx api-url ORGANIZATION-MEMBERSHIPS body ch
    :close? close?
    :response-key :organization_membership
    :make-tf make-read-membership-tf))

(defn update-membership
  [ctx api-url id body ch & {:keys [close?] :or {close? true}}]
  (update-resource ctx api-url ORGANIZATION-MEMBERSHIPS body id ch
    :close? close?
    :response-key :organization_membership
    :make-tf make-read-membership-tf))

(defn delete-membership
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (destroy-resource ctx api-url ORGANIZATION-MEMBERSHIPS id ch :close? close?
    :response-key :organization_membership
    :make-tf make-read-membership-tf))


(defn get-groups
  [ctx api-url ch & {:keys [close?] :or {close? true}}]
  (index-resource ctx api-url ORGANIZATION-GROUPS ch
    :close? close?
    :response-key :organization_groups
    :make-tf make-read-membership-tf))

(defn get-group
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (show-resource ctx api-url ORGANIZATION-GROUPS id ch
    :close? close?
    :response-key :organization_group
    :make-tf make-read-membership-tf))

(defn create-group
  [ctx api-url body ch & {:keys [close?] :or {close? true}}]
  (create-resource ctx api-url ORGANIZATION-GROUPS body ch
    :close? close?
    :response-key :organization_group
    :make-tf make-read-membership-tf))

(defn update-group
  [ctx api-url id body ch & {:keys [close?] :or {close? true}}]
  (update-resource ctx api-url ORGANIZATION-GROUPS body id ch
    :close? close?
    :response-key :organization_group
    :make-tf make-read-membership-tf))

(defn delete-group
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (destroy-resource ctx api-url ORGANIZATION-GROUPS id ch :close? close?
    :response-key :organization_group
    :make-tf make-read-membership-tf))

