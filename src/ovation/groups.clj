(ns ovation.groups
  (:require [ovation.routes :as routes]
            [ovation.util :as util :refer [<??]]
            [ovation.config :as config]
            [org.httpkit.client :as httpkit.client]
            [clojure.tools.logging :as logging]
            [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity! unprocessable-entity]]
            [ovation.constants :as k]
            [clojure.core.async :refer [chan >!! >! <! go promise-chan]]
            [slingshot.support :refer [get-throwable]]
            [ring.util.http-predicates :as hp]
            [ovation.http :as http]
            [slingshot.slingshot :refer [try+]]
            [ovation.request-context :as request-context]))

(defn make-read-team-group-tf
  [_]
  (fn [group]
    (select-keys group [:id :team_id :organization_group_id :role_id :name])))

(defn get-team-groups
  [ctx url team-id ch]
  (http/index-resource ctx url "team_groups" ch
    :response-key :team_groups
    :make-tf make-read-team-group-tf
    :query-params {:team_id team-id})

  ch)
