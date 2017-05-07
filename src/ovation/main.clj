(ns ovation.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [ovation.system :as system]
            [ovation.config :as config]
            [ovation.util :as util]))

(defn -main []
  (component/start (system/create-system {:web   {:port config/PORT}
                                          :authz {:v1-url (util/join-path [(config/SERVICES_API) "api" "v1"])
                                                  :v2-url (util/join-path [(config/SERVICES_API) "api" "v2"])}
                                          :db    {:host     (config/config :cloudant-db-url)
                                                  :username (config/config :cloudant-username)
                                                  :password (config/config :cloudant-password)}})))
