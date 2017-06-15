(ns ovation.test.system
  (:require [com.stuartsierra.component :as component]
            [ovation.config :as config]
            [ovation.system :as system]
            [ovation.util :as util]))

(def system-config
  {:web    {:port 3000}
   :authz  {:services-url (util/join-path [config/SERVICES_API_URL "api" "v2"])}
   :pubsub {:project-id (config/config :google-cloud-project-id :default "gcp-project-id")}
   :db     {:host     (config/config :cloudant-db-url :default "https://db-host")
            :username (config/config :cloudant-username :default "db-username")
            :password (config/config :cloudant-password :default "db-password")}})

(def test-system nil)


(defn init []
  (alter-var-root #'test-system
    (constantly (system/create-system system-config))))

(defn start []
  (alter-var-root #'test-system component/start))

(defn stop []
  (alter-var-root #'test-system
    #(when % (component/stop %))))

(defn go []
  (init)
  (start))

(defmacro system-background [ & body]
  `(try
     (go)
     (do ~@body)
     (finally
       (stop))))

(defn get-app []
  (get-in test-system [:api :handler]))

(defn get-db []
  (get-in test-system [:database]))
