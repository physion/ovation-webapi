(ns ovation.system
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as logging]
            [ovation.handler :as handler]
            [ovation.pubsub :as pubsub]
            (system.components
              [http-kit :as system-http-kit])
            [cemerick.url :as url]
            [ovation.authz :as authz]
            [qbits.spandex :as spandex]))

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
(defrecord Api [db authz search]
  component/Lifecycle

  (start [this]
    (logging/info "Starting API")
    (assoc this :handler (handler/create-app db authz search)))

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

;; Elasticsearch
(defrecord Spandex [url]
  component/Lifecycle
  (start [this]
    (logging/info "Starting Elasticsearch component")
    (assoc this :client (spandex/client {:hosts [(:url this)]})))

  (stop [this]
    (logging/info "Stopping Elasticsearch component")
    (assoc this :client nil)))

(defn new-elasticsearch [url]
  (map->Spandex {:url url}))

;; System
(defn create-system [config-options]
  (let [{:keys [web db authz pubsub elasticsearch]} config-options]
    (component/system-map
      :search (new-elasticsearch (:url elasticsearch))
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
              :authz :authz
              :search :search}))))
