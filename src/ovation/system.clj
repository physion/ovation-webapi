(ns ovation.system
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as logging]
            [ovation.handler :as handler]
            (system.components
              [jetty :refer [new-web-server]])
            [cemerick.url :as url]))


;; Database
(defrecord CouchDb [host username password connection]
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
(defrecord Api [api db]
  component/Lifecycle

  (start [this]
    (logging/info "Starting API")
    (assoc this :handler (handler/create-app db)))

  (stop [this]
    (logging/info "Stopping API")
    (assoc this :handler nil)))

(defn new-api []
  (map->Api {}))


;; System
(defn create-system [config-options]
  (let [{:keys [web db]} config-options]
    (component/system-map
      :database (new-database (:host db) (:username db) (:password db))
      :web (component/using
             (new-web-server (:port web))
             {:handler :api})
      :api (component/using
             (new-api)
             {:db :database}))))
