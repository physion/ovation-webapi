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


(def ORGANIZATIONS "organizations")
(def ORGANIZATION-MEMBERSHIPS "organization_memberships")
(def ORGANIZATION-GROUPS "organization_groups")
(def GROUP-MEMBERSHIPS "organization_group_memberships")


(defn request-opts
  [ctx]
  {:timeout     10000                                       ; ms
   :oauth-token (request-context/token ctx)
   :headers     {"Content-Type" "application/json; charset=utf-8"
                 "Accept"       "application/json"}})

(defn make-url
  [base & comps]
  (util/join-path (conj comps base)))

(defn make-org-links
  [ctx org]
  (let [rt (::request-context/routes ctx)]
    {:self                     (routes/self-route ctx "organization" (:id org))
     :projects                 (routes/org-projects-route rt (:id org))
     :organization-memberships (routes/org-memberships-route rt (:id org))
     :organization-groups      (routes/org-groups-route rt (:id org))}))


(defn make-read-org-tf
  [ctx]
  (fn [org]
    {:id    (:id org)
     :type  "Organization"
     :uuid  (:uuid org)
     :name  (:name org)
     :links (make-org-links ctx org)}))

(defn make-read-membership-tf
  [ctx]
  (fn [membership]
    (-> membership
      (assoc :type "OrganizationMembership")
      (assoc :links {:self (routes/self-route ctx "org-membership" (:id membership))}))))

(defn make-read-group-tf
  [ctx]
  (let [rt (::request-context/routes ctx)
        org-id (::request-context/org ctx)]
    (fn [group]
      (-> group
        (assoc :type "OrganizationGroup")
        (assoc :links {:self              (routes/self-route ctx "org-group" (:id group))
                       :group-memberships (routes/group-memberships-route rt org-id (:id group))})))))

(defn make-read-group-membership-tf
  [ctx]
  (let [params   (:params (::request-context/request ctx))
        group-id (:id params)
        rt       (:ovation.request-context/routes ctx)
        org      (:ovation.request-context/org ctx)]
    (fn [membership]
      (-> membership
        (assoc :type "OrganizationGroupMembership")
        (assoc :links {:self (rt :get-group-membership {:org org :id group-id :membership-id (:id membership)})})))))



(defn read-collection-tf
  [ctx key make-tf]
  (fn
    [response]
    (if (util/response-exception? response)
      response
      (let [orgs (key response)]
        (map (make-tf ctx) orgs)))))

(defn read-single-tf
  [ctx key make-tf]
  (let [tf (make-tf ctx)]
    (fn [response]
      (if (util/response-exception? response)
        response
        (let [obj    (key response)
              result (tf obj)]
          result)))))

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

(defn get-organizations
  "Gets all organizations for authenticated user onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys a list of Organizations or Throwable."
  [ctx api-url ch & {:keys [close?] :or {close? true}}]

  (index-resource ctx api-url ORGANIZATIONS ch
    :close? close?
    :response-key :organizations
    :make-tf make-read-org-tf))

(defn get-organizations*
  [ctx api-url]
  (let [ch (chan)]
    (get-organizations ctx api-url ch)
    {:organizations (<?? ch)}))

(defn get-organization
  "Gets a single organization onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys an Organization or Throwable"
  [ctx api-url ch org-id & {:keys [close?] :or {close? true}}]

  (show-resource ctx api-url ORGANIZATIONS org-id ch
    :close? close?
    :response-key :organization
    :make-tf make-read-org-tf))

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

   Conveys an Organization or a response exception map"
  [ctx api-url ch org & {:keys [close?] :or {close? true}}]

  (let [{org-id :id
         name   :name} org
        body {:organization {:id   org-id
                             :name name}}]

    (update-resource ctx api-url ORGANIZATIONS body org-id ch
      :close? close?
      :response-key :organization
      :make-tf make-read-org-tf)))

(defn update-organization*
  [ctx api-url body]
  (let [ch (chan)]
    (update-organization ctx api-url ch (:organization body))
    (let [org (<?? ch)]
      {:organization org})))

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
    :make-tf make-read-group-tf))

(defn get-group
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (show-resource ctx api-url ORGANIZATION-GROUPS id ch
    :close? close?
    :response-key :organization_group
    :make-tf make-read-group-tf))

(defn create-group
  [ctx api-url body ch & {:keys [close?] :or {close? true}}]
  (create-resource ctx api-url ORGANIZATION-GROUPS body ch
    :close? close?
    :response-key :organization_group
    :make-tf make-read-group-tf))

(defn update-group
  [ctx api-url id body ch & {:keys [close?] :or {close? true}}]
  (update-resource ctx api-url ORGANIZATION-GROUPS body id ch
    :close? close?
    :response-key :organization_group
    :make-tf make-read-group-tf))

(defn delete-group
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (destroy-resource ctx api-url ORGANIZATION-GROUPS id ch :close? close?
    :response-key :organization_group
    :make-tf make-read-group-tf))



(defn get-group-memberships
  [ctx api-url group-id ch & {:keys [close?] :or {close? true}}]
  (index-resource ctx api-url GROUP-MEMBERSHIPS ch
    :query-params {:group_id group-id}
    :close? close?
    :response-key :group_memberships                        ;; TODO
    :make-tf make-read-group-membership-tf))

(defn get-group-membership
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (show-resource ctx api-url GROUP-MEMBERSHIPS id ch
    :close? close?
    :response-key :group_membership                         ;; TODO
    :make-tf make-read-group-membership-tf))

(defn create-group-membership
  [ctx api-url body ch & {:keys [close?] :or {close? true}}]
  (create-resource ctx api-url GROUP-MEMBERSHIPS body ch
    :close? close?
    :response-key :group_membership
    :make-tf make-read-group-membership-tf))

(defn update-group-membership
  [ctx api-url id body ch & {:keys [close?] :or {close? true}}]
  (update-resource ctx api-url GROUP-MEMBERSHIPS body id ch
    :close? close?
    :response-key :group_membership
    :make-tf make-read-group-membership-tf))

(defn delete-group-membership
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (destroy-resource ctx api-url GROUP-MEMBERSHIPS id ch :close? close?
    :response-key :group_membership
    :make-tf make-read-group-membership-tf))

