(ns ovation.authz
  (:require [ovation.organizations :as organizations]
            [clojure.tools.logging :as logging]
            [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan promise-chan]]
            [ovation.util :refer [<??]]
            [ovation.teams :as teams]))

(defprotocol AuthzApi
  "Authorization service"
  (get-organizations [this ctx])
  (create-organization [this ctx body])
  (get-organization [this ctx])
  (update-organization [this ctx body])

  (get-authorizations [this ctx])

  (get-organization-memberships [this ctx])
  (create-organization-membership [this ctx body])
  (get-organization-membership [this ctx id])
  (put-organization-membership [this ctx id body])
  (delete-organization-membership [this ctx id])

  (get-organization-groups [this ctx])
  (create-organization-group [this ctx body])
  (get-organization-group [this ctx id])
  (put-organization-group [this ctx id body])
  (delete-organization-group [this ctx id])

  (get-organization-groups-memberships [this ctx group-id])
  (create-organization-group-membership [this ctx body])
  (get-organization-group-membership [this ctx id])
  (put-organization-group-membership [this ctx id body])
  (delete-organization-group-membership [this ctx id]))

;; Organizations, Groups
(defrecord AuthzService [v1-url v2-url]
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
    (organizations/get-organizations* ctx (:v2-url this)))
  (create-organization [this ctx body]
    (let [ch (chan)]
      (organizations/create-organization ctx (:v2-url this) (:organization body) ch)
      (let [new-org (<?? ch)]
        {:organization new-org})))
  (get-organization [this ctx]
    (organizations/get-organization* ctx (:v2-url this)))
  (update-organization [this ctx body]
    (organizations/update-organization* ctx (:v2-url this) body))

  ;; ORGANIZATION MEMBERSHIPS
  (get-organization-memberships [this ctx]
    (let [ch (chan)]
      (organizations/get-memberships ctx (:v2-url this) ch)
      (let [memberships (<?? ch)]
        {:organization-memberships memberships})))

  (create-organization-membership [this ctx body]
    (let [ch (chan)]
      (organizations/create-membership ctx (:v2-url this) (:organization-membership body) ch)
      (let [membership (<?? ch)]
        {:organization-membership membership})))

  (get-organization-membership [this ctx id]
    (let [ch (chan)]
      (organizations/get-membership ctx (:v2-url this) id ch)
      (let [membership (<?? ch)]
        {:organization-membership membership})))

  (put-organization-membership [this ctx id body]
    (let [ch (chan)]
      (organizations/update-membership ctx (:v2-url this) id (:organization-membership body) ch)
      (let [membership (<?? ch)]
        {:organization-membership membership})))

  (delete-organization-membership [this ctx id]
    (let [ch (chan)]
      (organizations/delete-membership ctx (:v2-url this) id ch)
      (let [result (<?? ch)]
        result)))

  ;; GROUPS
  (get-organization-groups [this ctx]
    (let [ch (chan)]
      (organizations/get-groups ctx (:v2-url this) ch)
      (let [groups (<?? ch)]
        {:organization-groups groups})))

  (create-organization-group [this ctx body]
    (let [ch (chan)]
      (organizations/create-group ctx (:v2-url this) (:organization-group body) ch)
      (let [group (<?? ch)]
        {:organization-group group})))

  (get-organization-group [this ctx id]
    (let [ch (chan)]
      (organizations/get-group ctx (:v2-url this) id ch)
      (let [group (<?? ch)]
        {:organization-group group})))

  (put-organization-group [this ctx id body]
    (let [ch (chan)]
      (organizations/update-group ctx (:v2-url this) id (:organization-group body) ch)
      (let [group (<?? ch)]
        {:organization-group group})))

  (delete-organization-group [this ctx id]
    (let [ch (chan)]
      (organizations/delete-group ctx (:v2-url this) id ch)
      (let [result (<?? ch)]
        result)))

  ;; GROUP MEMBERSHIPS
  (get-organization-groups-memberships [this ctx group-id]
    (let [ch (chan)]
      (organizations/get-group-memberships ctx (:v2-url this) group-id ch)
      (let [memberships (<?? ch)]
        {:group-memberships memberships})))

  (create-organization-group-membership [this ctx body]
    (let [ch (chan)]
      (organizations/create-group-membership ctx (:v2-url this) (:group-membership body) ch)
      (let [group (<?? ch)]
        {:group-membership group})))

  (get-organization-group-membership [this ctx id]
    (let [ch (chan)]
      (organizations/get-group-membership ctx (:v2-url this) id ch)
      (let [group (<?? ch)]
        {:group-membership group})))


  (put-organization-group-membership [this ctx id body]
    (let [ch (chan)]
      (organizations/update-group-membership ctx (:v2-url this) id (:group-membership body) ch)
      (let [group (<?? ch)]
        {:group-membership group})))

  (delete-organization-group-membership [this ctx id]
    (let [ch (chan)]
      (organizations/delete-group-membership ctx (:v2-url this) id ch)
      (let [result (<?? ch)]
        result)))

  (get-authorizations [this ctx]
    (let [ch (promise-chan)]
      (teams/get-authorizations ctx (:v2-url this) ch)      ;;TODO
      (let [result (<?? ch)]
        (println result)
        {:authorization result}))))

(defn new-authz-service [v1-url v2-url]
  (map->AuthzService {:v1-url v1-url :v2-url v2-url}))
