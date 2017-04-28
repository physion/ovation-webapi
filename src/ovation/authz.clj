(ns ovation.authz
  (:require [ovation.organizations :as organizations]
            [clojure.tools.logging :as logging]
            [com.stuartsierra.component :as component]))

(defprotocol AuthzApi
  "Authorization service"
  (get-organizations [this ctx])
  (get-organization [this ctx])
  (update-organization [this ctx body]))

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
    (organizations/update-organization* ctx (:v1-url this) body)))

(defn new-authz-service [v1-url v2-url]
  (map->AuthzService {:v1-url v1-url :v2-url v2-url}))
