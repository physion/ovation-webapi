(ns ovation.user
  (:require [ovation.system :as system]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]
            [ovation.config :as config]
            [ovation.util :as util]))

(def system-config
  {:web           {:port 3000}
   :elasticsearch {:url (config/config :elasticsearch-url)}
   :pubsub        {:project-id (config/config :google-cloud-project-id :default "gcp-project-id")}
   :authz         {:services-url (util/join-path [config/SERVICES_API_URL "api" "v2"])}
   :flyway        {:locations ["filesystem:/app/db/migrations"]}
   :db            {:adapter       "mysql"
                   :username      (config/config :mysql-username)
                   :password      (config/config :mysql-password)
                   :database-name (config/config :mysql-database-name)
                   :server-name   (config/config :mysql-server)
                   :port-number   (config/config :mysql-port)}})

(def dev-system nil)

(defn init []
  (alter-var-root #'dev-system
    (constantly (system/create-system system-config))))

(defn start []
  (alter-var-root #'dev-system component/start))

(defn stop []
  (alter-var-root #'dev-system
    #(when % (component/stop %))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (set-refresh-dirs "src")
  (refresh :after 'ovation.user/go))
