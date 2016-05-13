(ns ovation.tokens
  (:require [org.httpkit.client :as http]
            [ovation.config :as config]
            [ovation.util :as util]
            [clojure.data.json :as json]
            [clojure.walk :as walk]))

(defn get-token
  "Gets a new authorization token from the AUTH_SERVER, returning the full HTTP response"
  [email password]
  (let [body    (json/write-str (walk/stringify-keys {:email email :password password}))
        options {:body body
                 :headers {"Content-Type" "application/json"}}
        url (util/join-path [config/AUTH_SERVER "api" "v1" "sessions"])
        response (http/post url options)
        body-json (walk/keywordize-keys (json/read-str (:body @response)))
        status (int (:status @response))]
    (-> @response
      (dissoc :opts)
      (assoc :status status)
      (assoc :body body-json))))

(defn refresh-token
  [request]
  nil)
