(ns ovation.config)

(def LOGGING_HOST (or (System/getenv "LOGGING_HOST") (System/getProperty "LOGGING_HOST")))

(def LOGGING_NAME (or (System/getenv "LOGGING_NAME") (System/getProperty "LOGGING_NAME")))
