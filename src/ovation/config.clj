(ns ovation.config)

(def AUTH_SERVER (or (System/getenv "AUTH_SERVER") (System/getProperty "AUTH_SERVER")))
(def LOGGING_HOST (or (System/getenv "LOGGING_HOST") (System/getProperty "LOGGING_HOST")))
(def LOGGING_NAME (or (System/getenv "LOGGING_NAME") (System/getProperty "LOGGING_NAME")))
