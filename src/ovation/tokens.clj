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
                 :accept "application/json; charset=utf-8"
                 :conent-type "application/json; charset=utf-8"}
        url (util/join-path [config/AUTH_SERVER "api" "v1" "sessions"])
        _ (println url)
        response (http/post url options)]
    (println @response)
    @response))

(defn refresh-token
  [request]
  nil)
