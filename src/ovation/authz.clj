(ns ovation.authz
  (:require [ovation.organizations :as organizations]
            [clojure.tools.logging :as logging]
            [com.stuartsierra.component :as component]
            [clojure.core.async :refer [chan]]
            [ovation.util :refer [<??]]))

(defprotocol AuthzApi
  "Authorization service"
  (get-organizations [this ctx])
  (get-organization [this ctx])
  (update-organization [this ctx body])

  (get-organization-memberships [this ctx])
  (create-organization-membership [this ctx body])
  (get-organization-membership [this ctx id])
  (put-organization-membership [this ctx id body])
  (delete-organization-membership [this ctx id]))

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
  (get-organizations [this ctx]
    (organizations/get-organizations* ctx (:v1-url this)))
  (get-organization [this ctx]
    (organizations/get-organization* ctx (:v1-url this)))
  (update-organization [this ctx body]
    (organizations/update-organization* ctx (:v1-url this) body))

  (get-organization-memberships [this ctx]
    (let [ch (chan)]
      (organizations/get-memberships ctx (:v2-url this) ch)
      (let [memberships (<?? ch)]
        {:organization-memberships memberships})))

  (create-organization-membership [this ctx body]
    (let [ch (chan)]
      (organizations/create-membership ctx (:v2-url this) (:organization-membership body) ch)
      (let [memberships (<?? ch)]
        {:organization-membershis memberships})))

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
        result))))

(defn new-authz-service [v1-url v2-url]
  (map->AuthzService {:v1-url v1-url :v2-url v2-url}))