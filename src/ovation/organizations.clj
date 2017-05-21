(ns ovation.organizations
  (:require [ovation.routes :as routes]
            [ovation.http :refer [index-resource
                                  show-resource
                                  create-resource
                                  update-resource
                                  destroy-resource]]
            [ovation.util :as util :refer [<??]]
            [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity!]]
            [ovation.request-context :as request-context]
            [slingshot.support :refer [get-throwable]]
            [slingshot.slingshot :refer [try+]]
            [clojure.core.async :refer [chan go >! >!! pipeline pipe]]))


(def ORGANIZATIONS "organizations")
(def ORGANIZATION-MEMBERSHIPS "organization_memberships")
(def ORGANIZATION-GROUPS "organization_groups")
(def GROUP-MEMBERSHIPS "organization_group_memberships")


(defn make-org-links
  [ctx org]
  (let [rt (::request-context/routes ctx)]
    {:self                     (routes/self-route ctx "organization" (:id org) (:id org))
     :projects                 (routes/org-projects-route rt (:id org))
     :organization-memberships (routes/org-memberships-route rt (:id org))
     :organization-groups      (routes/org-groups-route rt (:id org))}))


(defn make-read-org-tf
  [ctx]
  (fn [org]
    (let [result {:id                       (:id org)
                  :type                     "Organization"
                  :uuid                     (:uuid org)
                  :name                     (:name org)
                  :is_admin                 (:is_admin org)
                  :research_subscription_id (:research_subscription_id org)
                  :links                    (make-org-links ctx org)}]
      (util/remove-nil-values result))))


(defn make-read-membership-tf
  [ctx]
  (fn [membership]
    (let [filtered-membership (select-keys membership [:id :organization_id :role :email :first_name :last_name :job_title :contact_information])
          membership-without-nil-values (into {} (filter second filtered-membership))]
      (-> membership-without-nil-values
        (assoc :type "OrganizationMembership")
        (assoc :links {:self (routes/self-route ctx "org-membership" (:id membership))})))))

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



(defn get-organizations
  "Gets all organizations for authenticated user onto the provided channel. If close?, channel is closed on completion (default true).
   Conveys a list of Organizations or Throwable."
  [ctx api-url ch & {:keys [close?] :or {close? true}}]

  (index-resource ctx api-url ORGANIZATIONS ch
    :close? close?
    :response-key :organizations
    :make-tf make-read-org-tf))

(defn create-organization
  [ctx api-url new-org ch & {:keys [close?] :or {close? true}}]

  (create-resource ctx api-url ORGANIZATIONS new-org ch
    :close? close?
    :response-key :organization
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
  (let [org-id (::request-context/org ctx)]
    (index-resource ctx api-url ORGANIZATION-MEMBERSHIPS ch
      :close? close?
      :query-params {:organization_id org-id}
      :response-key :organization_memberships
      :make-tf make-read-membership-tf)))

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
    :query-params {:organization_id (::request-context/org ctx)}
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
    :response-key :organization_group_memberships
    :make-tf make-read-group-membership-tf))

(defn get-group-membership
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (show-resource ctx api-url GROUP-MEMBERSHIPS id ch
    :close? close?
    :response-key :organization_group_membership
    :make-tf make-read-group-membership-tf))

(defn create-group-membership
  [ctx api-url body ch & {:keys [close?] :or {close? true}}]
  (create-resource ctx api-url GROUP-MEMBERSHIPS body ch
    :close? close?
    :response-key :organization_group_membership
    :make-tf make-read-group-membership-tf))

(defn update-group-membership
  [ctx api-url id body ch & {:keys [close?] :or {close? true}}]
  (update-resource ctx api-url GROUP-MEMBERSHIPS body id ch
    :close? close?
    :response-key :organization_group_membership
    :make-tf make-read-group-membership-tf))

(defn delete-group-membership
  [ctx api-url id ch & {:keys [close?] :or {close? true}}]
  (destroy-resource ctx api-url GROUP-MEMBERSHIPS id ch :close? close?
    :response-key :organization_group_membership
    :make-tf make-read-group-membership-tf))

(defn transfer-project
  [ctx proj-uuid dest-org])
