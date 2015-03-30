(ns ovation.config)

(def LOGGING_HOST (or (System/getenv "LOGGING_HOST") (System/getProperty "LOGGING_HOST")))
(def AUTH_SERVER (or (System/getenv "AUTH_SERVER") (System/getProperty "AUTH_SERVER")))
