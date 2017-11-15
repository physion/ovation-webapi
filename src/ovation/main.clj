(ns ovation.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [ovation.system :as system]
            [ovation.config :as config]
            [ovation.util :as util]
            [clojure.tools.logging :as logging]))

(defn -main []
  (component/start (system/create-system {:web    {:port config/PORT}
                                          :elasticsearch {:url (config/config :elasticsearch-url)}
                                          :pubsub {:project-id (config/config :google-cloud-project-id)}
                                          :authz  {:services-url (util/join-path [config/SERVICES_API_URL "api" "v2"])}
                                          :db     {:host     (config/config :cloudant-db-url)
                                                   :username (config/config :cloudant-username)
                                                   :password (config/config :cloudant-password)}})))
