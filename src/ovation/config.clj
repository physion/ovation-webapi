(ns ovation.config
  (:require [environ.core :refer [env]]))

(defn config
  [name & {:keys [default] :or {default nil}}]
  (or (env name) default))

(def JWT_SECRET (config :jwt-secret))
(def NOTIFICATIONS_SERVER (config :notifications-server))
(def AUTH_SERVER (config :auth-server))

(def RESOURCES_SERVER (config :resources-server))

(def TEAMS_SERVER (config :teams-server))

(def LOGGING_HOST (config :logging-host))
(def LOGGING_PORT (config :logging-port))

(def CLOUDANT_DB_URL (config :cloudant-db-url))
(def CLOUDANT_USERNAME (config :clouddant-username))
(def CLOUDANT_PASSWORD (config :cloudant-password))

(def PORT (config :port :default 8080))
(def ZIP_SERVICE (config :zip-service :default "https://zip-staging.ovation.io"))
