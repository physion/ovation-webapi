(ns ovation.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [ovation.system :as system]
            [ovation.config :as config]))

(defn -main []
  (component/start (system/create-system {:web {:port config/PORT}
                                          :db  {:host     (config/config :cloudant-db-url)
                                                :username (config/config :cloudant-username)
                                                :password (config/config :cloudant-password)}})))
