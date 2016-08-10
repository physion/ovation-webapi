(ns ovation.config)

(defn config
  [name & {:keys [default] :or {default nil}}]
  (or (System/getenv name) (System/getProperty name) default))

(def JWT_SECRET (config "JWT_SECRET"))
(def NOTIFICATIONS_SERVER (config "NOTIFICATIONS_SERVER"))
(def AUTH_SERVER (config "AUTH_SERVER"))

(def RESOURCES_SERVER (config "RESOURCES_SERVER"))

(def TEAMS_SERVER (config "TEAMS_SERVER"))

(def LOGGING_HOST (config "LOGGING_HOST"))
(def LOGGING_PORT (config "LOGGING_PORT"))

(def CLOUDANT_DB_URL (config "CLOUDANT_DB_URL"))
(def CLOUDANT_USERNAME (config "CLOUDANT_USERNAME"))
(def CLOUDANT_PASSWORD (config "CLOUDANT_PASSWORD"))

(def PORT (config "PORT" :default 8080))
(def ZIP_SERVICE (config "ZIP_SERVICE" :default "https://zip-staging.ovation.io"))
