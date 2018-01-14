(ns ovation.system
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as logging]
            [ovation.handler :as handler]
            [ovation.pubsub :as pubsub]
            (system.components
              [http-kit :as system-http-kit])
            [cemerick.url :as url]
            [ovation.authz :as authz]
            [qbits.spandex :as spandex]
            [palikka.components
             [flyway :as palikka-flyway]
             [database :as palikka-database]]
            [ovation.config :as config]))

;; Database
(defrecord Database [jdbc pubsub]
  component/Lifecycle

  (start [this]
    (logging/info "Starting database")
    (assoc this :db-spec (-> jdbc :db)))

  (stop [this]
    (logging/info "Stopping database")
    (assoc this :db-spec nil)))

(defn new-database []
  (map->Database {}))


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
    (let [u (cemerick.url/url (:url this))
          {user :username
           password :password} u
          base-url (str (-> u (assoc :username nil) (assoc :password nil)))]
      (logging/info "Starting Elasticsearch component")
      (assoc this :client (spandex/client {:hosts       [base-url]
                                           :http-client (if (and user password)
                                                          {:basic-auth {:user     user
                                                                        :password password}}
                                                          {})}))))

  (stop [this]
    (logging/info "Stopping Elasticsearch component")
    (assoc this :client nil)))

(defn new-elasticsearch [url]
  (map->Spandex {:url url}))


;; System
(defn create-system [config-options]
  (let [{:keys [web db authz pubsub elasticsearch flyway]} config-options]
    (component/system-map
      :search (new-elasticsearch (:url elasticsearch))
      :flyway (component/using
                (palikka-flyway/migrate {:schemas   (:schemas flyway)
                                         :locations (:locations flyway)})
                {:db :jdbc})
      :jdbc (palikka-database/create {:pool-name          (get db :pool-name "db-pool")
                                      :adapter            (get db :adapter "mysql")
                                      :username           (:username db)
                                      :password           (:password db)
                                      :database-name      (:database-name db)
                                      :server-name        (:server-name db)
                                      :port-number        (:port-number db)})
      :database (component/using
                  (new-database)
                  {:pubsub :pubsub
                   :jdbc   :jdbc})
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

