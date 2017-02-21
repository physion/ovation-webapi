(ns ovation.user
  (:require [ovation.system :as system]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [ovation.config :as config]))

(def system-config
  {:web {:port 3000}
   :db  {:host     (config/config :cloudant-db-url)
         :port     1234
         :username (config/config :cloudant-username)
         :password (config/config :cloudant-password)}})

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
  (refresh :after 'ovation.user/go))
