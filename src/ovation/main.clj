(ns ovation.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [ovation.system :as system]
            [ovation.config :as config]
            [ovation.util :as util]))

(defn -main []
  (component/start (system/create-system {:web           {:port config/PORT}
                                          :elasticsearch {:url (config/config :elasticsearch-url)}
                                          :pubsub        {:project-id (config/config :google-cloud-project-id)}
                                          :authz         {:services-url (util/join-path [config/SERVICES_API_URL "api" "v2"])}
                                          :flyway        {:locations ["filesystem:/app/db/migrations"]}
                                          :db            {:adapter       "mysql"
                                                          :username      (config/config :mysql-username)
                                                          :password      (config/config :mysql-password)
                                                          :database-name (config/config :mysql-database-name)
                                                          :server-name   (config/config :mysql-server)
                                                          :port-number   (config/config :mysql-port)}})))
