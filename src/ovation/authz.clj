(ns ovation.authz
  (:require [ovation.organizations :as organizations]
            [clojure.tools.logging :as logging]
            [com.stuartsierra.component :as component]
            [clojure.core.async :as async :refer [chan go <!]]
            [ovation.util :refer [<??]]
            [ovation.http :as http]
            [ovation.groups :as groups]))


(defprotocol AuthzApi
  "Authorization service"
  (get-organizations [this ctx])
  (create-organization [this ctx body])
  (get-organization [this ctx])
  (update-organization [this ctx body])
  (delete-organization [this ctx])

  (get-authorization [this ctx])
  (get-authorization-ch [this ctx])

  (get-organization-memberships [this ctx])
  (create-organization-membership [this ctx body])
  (get-organization-membership [this ctx id])
  (put-organization-membership [this ctx id body])
  (delete-organization-membership [this ctx id])

  (get-organization-member-project-ids [this ctx member-id])

  (get-organization-groups [this ctx])
  (create-organization-group [this ctx body])
  (get-organization-group [this ctx id])
  (put-organization-group [this ctx id body])
  (delete-organization-group [this ctx id])
  (get-organization-group-project-ids [this ctx group-id])

  (get-organization-groups-memberships [this ctx group-id])
  (create-organization-group-membership [this ctx body])
  (get-organization-group-membership [this ctx id])
  (put-organization-group-membership [this ctx id body])
  (delete-organization-group-membership [this ctx id])

  (get-team-groups [this ctx team-id])
  (post-team-group [this ctx body])
  (get-team-group [this ctx group-id])
  (put-team-group [this ctx group-id body])
  (delete-team-group [this ctx group-id]))

(defn get-authorizations
  [ctx url-base ch]

  (let [org-id (:ovation.request-context/org ctx)]
    (http/show-resource ctx url-base "authorizations" org-id ch
      :response-key :authorization))

  ch)

;; Organizations, Groups
(defrecord AuthzService [services-url authorizations]

  component/Lifecycle
  (start [this]
    (logging/info "Starting Authz service")
    this)

  (stop [this]
    (logging/info "Stopping Authz service")
    this)

  AuthzApi

  ;; ORGANIZATIONS
  (get-organizations [this ctx]
    (organizations/get-organizations* ctx (:services-url this)))
  (create-organization [this ctx body]
    (let [ch (chan)]
      (organizations/create-organization ctx (:services-url this) (:organization body) ch)
      (let [new-org (<?? ch)]
        {:organization new-org})))
  (get-organization [this ctx]
    (organizations/get-organization* ctx (:services-url this)))

  (update-organization [this ctx body]
    (organizations/update-organization* ctx (:services-url this) body))

  (delete-organization [this ctx]
    (let [ch (chan)]
      (organizations/delete-organization ctx (:services-url this) ch)
      (let [result (<?? ch)]
        result)))

  ;; ORGANIZATION MEMBERSHIPS
  (get-organization-memberships [this ctx]
    (let [ch (chan)]
      (organizations/get-memberships ctx (:services-url this) ch)
      (let [memberships (<?? ch)]
        {:organization-memberships memberships})))

  (create-organization-membership [this ctx body]
    (let [ch (chan)]
      (organizations/create-membership ctx (:services-url this) (:organization-membership body) ch)
      (let [membership (<?? ch)]
        {:organization-membership membership})))

  (get-organization-membership [this ctx id]
    (let [ch (chan)]
      (organizations/get-membership ctx (:services-url this) id ch)
      (let [membership (<?? ch)]
        {:organization-membership membership})))

  (put-organization-membership [this ctx id body]
    (let [ch (chan)]
      (organizations/update-membership ctx (:services-url this) id (:organization-membership body) ch)
      (let [membership (<?? ch)]
        {:organization-membership membership})))

  (delete-organization-membership [this ctx id]
    (let [ch (chan)]
      (organizations/delete-membership ctx (:services-url this) id ch)
      (let [result (<?? ch)]
        result)))

  ;; GROUPS
  (get-organization-groups [this ctx]
    (let [ch (chan)]
      (organizations/get-groups ctx (:services-url this) ch)
      (let [groups (<?? ch)]
        {:organization-groups groups})))

  (create-organization-group [this ctx body]
    (let [ch (chan)]
      (organizations/create-group ctx (:services-url this) (:organization-group body) ch)
      (let [group (<?? ch)]
        {:organization-group group})))

  (get-organization-group [this ctx id]
    (let [ch (chan)]
      (organizations/get-group ctx (:services-url this) id ch)
      (let [group (<?? ch)]
        {:organization-group group})))

  (put-organization-group [this ctx id body]
    (let [ch (chan)]
      (organizations/update-group ctx (:services-url this) id (:organization-group body) ch)
      (let [group (<?? ch)]
        {:organization-group group})))

  (delete-organization-group [this ctx id]
    (let [ch (chan)]
      (organizations/delete-group ctx (:services-url this) id ch)
      (let [result (<?? ch)]
        result)))

  ;; GROUP MEMBERSHIPS
  (get-organization-groups-memberships [this ctx group-id]
    (let [ch (chan)]
      (organizations/get-group-memberships ctx (:services-url this) group-id ch)
      (let [memberships (<?? ch)]
        {:group-memberships memberships})))

  (create-organization-group-membership [this ctx body]
    (let [ch (chan)]
      (organizations/create-group-membership ctx (:services-url this) (:group-membership body) ch)
      (let [group (<?? ch)]
        {:group-membership group})))

  (get-organization-group-membership [this ctx id]
    (let [ch (chan)]
      (organizations/get-group-membership ctx (:services-url this) id ch)
      (let [group (<?? ch)]
        {:group-membership group})))


  (put-organization-group-membership [this ctx id body]
    (let [ch (chan)]
      (organizations/update-group-membership ctx (:services-url this) id (:group-membership body) ch)
      (let [group (<?? ch)]
        {:group-membership group})))

  (delete-organization-group-membership [this ctx id]
    (let [ch (chan)]
      (organizations/delete-group-membership ctx (:services-url this) id ch)
      (let [result (<?? ch)]
        result)))


  (get-authorization-ch [this ctx]
    (let [org (:ovation.request-context/org ctx)]
      (let [result-ch (if-let [existing-ch (get-in this [:authorizations org])]
                        existing-ch
                        (let [ch (async/promise-chan)]
                          (get-authorizations ctx services-url ch)
                          (assoc-in this [:authorizations org] ch)
                          ch))]
        result-ch)))

  (get-authorization [this ctx]
    (let [result-ch (get-authorization-ch this ctx)]
      {:authorization (<?? result-ch)}))

  (get-team-groups [this ctx team-id]
    (let [ch (chan)]
      (groups/get-team-groups ctx (:services-url this) team-id ch)
      {:team-groups (<?? ch)}))

  (post-team-group [this ctx body]
    (let [ch (chan)]
      (groups/create-team-group ctx (:services-url this) {:team_group (:team-group body)} ch)
      {:team-group (<?? ch)}))

  (get-team-group [this ctx group-id]
    (let [ch (chan)]
      (groups/get-team-group ctx (:services-url this) group-id ch)
      {:team-group (<?? ch)}))

  (put-team-group [this ctx group-id body]
    (let [ch (chan)]
      (groups/update-team-group ctx (:services-url this) group-id {:team_group (:team-group body)} ch)
      {:team-group (<?? ch)}))

  (delete-team-group [this ctx group-id]
    (let [ch (chan)]
      (groups/delete-team-group ctx (:services-url this) group-id ch)
      (<?? ch)))

  (get-organization-group-project-ids [this ctx group-id]
    (let [ch (chan)]
      (organizations/group-project-ids ctx (:services-url this) group-id ch)
      (<?? ch)))

  (get-organization-member-project-ids [this ctx member-id]
    (let [ch (chan)])))


(defn new-authz-service [services-url]
  (map->AuthzService {:services-url   services-url
                      :authorizations {}}))
