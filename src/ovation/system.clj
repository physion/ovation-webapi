(ns ovation.system
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as logging]
            [ovation.handler :as handler]
            [ovation.pubsub :as pubsub]
            (system.components
              [http-kit :as system-http-kit])
            [cemerick.url :as url]
            [ovation.authz :as authz]))

;; Database
(defrecord CouchDb [host username password pubsub connection]
  component/Lifecycle

  (start [this]
    (logging/info "Starting database")
    (assoc this :connection (-> (url/url host)
                              (assoc :username username
                                     :password password))))

  (stop [this]
    (logging/info "Stopping database")
    (assoc this :connection nil)))

(defn new-database [host username password]
  (map->CouchDb {:host host :username username :password password}))


;; API
(defrecord Api [db authz]
  component/Lifecycle

  (start [this]
    (logging/info "Starting API")
    (assoc this :handler (handler/create-app db authz)))

  (stop [this]
    (logging/info "Stopping API")
    (assoc this :handler nil)))

(defn new-api []
  (map->Api {}))


;; Notifications
(defrecord Notifications [url]
  component/Lifecycle

  (start [this]
    (logging/info "Starting notifiations component")
    this)

  (stop [this]
    (logging/info "Stopping notifications component")
    this))

;; System
(defn create-system [config-options]
  (let [{:keys [web db authz pubsub]} config-options]
    (component/system-map
      :database (component/using
                  (new-database (:host db) (:username db) (:password db))
                  {:pubsub :pubsub})
      :web (component/using
             (system-http-kit/new-web-server (:port web))
             {:handler :api})
      :authz (authz/new-authz-service (:services-url authz))
      :pubsub (pubsub/new-pubsub (:project-id pubsub))
      :api (component/using
             (new-api)
             {:db    :database
              :authz :authz}))))
