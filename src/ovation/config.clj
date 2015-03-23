(ns ovation.config)

(def LOGGING_HOST (or (System/getenv "LOGGING_HOST") (System/getProperty "LOGGING_HOST")))
