(ns ovation.config
  (:require [environ.core :refer [env]]
            [ovation.util :as util]))

(defn config
  [name & {:keys [default] :or {default nil}}]
  (or (env name) default))

(def PORT (Integer/parseInt (str (config :port :default 3000))))

(def JWT_SECRET (config :jwt-secret))

(def SERVICES_API_URL (config :ovation-io-host-uri :default "https://services-staging.ovation.io"))

(def NOTIFICATIONS_SERVER SERVICES_API_URL)
(def AUTH_SERVER SERVICES_API_URL)
(def RESOURCES_SERVER SERVICES_API_URL)
(def TEAMS_SERVER (util/join-path [SERVICES_API_URL "api" "v2"]))
(def ORGS_SERVER TEAMS_SERVER)

(def ZIP_SERVICE (config :zip-service :default "https://zip-staging.ovation.io"))
