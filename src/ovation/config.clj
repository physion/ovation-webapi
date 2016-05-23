(ns ovation.config)

(def AUTH_SERVER (or (System/getenv "AUTH_SERVER") (System/getProperty "AUTH_SERVER")))

(def RESOURCES_SERVER (or (System/getenv "RESOURCES_SERVER") (System/getProperty "RESOURCES_SERVER")))

(def TEAMS_SERVER (or (System/getenv "TEAMS_SERVER") (System/getProperty "TEAMS_SERVER")))

(def LOGGING_HOST (or (System/getenv "LOGGING_HOST") (System/getProperty "LOGGING_HOST")))
(def LOGGING_PORT (or (System/getenv "LOGGING_PORT") (System/getProperty "LOGGING_PORT")))
(def LOGGING_NAME (or (System/getenv "LOGGING_NAME") (System/getProperty "LOGGING_NAME")))

(def CLOUDANT_DB_URL (or (System/getenv "CLOUDANT_DB_URL") (System/getProperty "CLOUDANT_DB_URL")))
(def CLOUDANT_USERNAME (or (System/getenv "CLOUDANT_USERNAME") (System/getProperty "CLOUDANT_USERNAME")))
(def CLOUDANT_PASSWORD (or (System/getenv "CLOUDANT_PASSWORD") (System/getProperty "CLOUDANT_PASSWORD")))

(defn config
  [name]
  (or (System/getenv name) (System/getProperty name)))

(def JWT_SECRET (config "JWT_SECRET"))
(def NOTIFICATIONS_SERVER (config "NOTIFICATIONS_SERVER"))
