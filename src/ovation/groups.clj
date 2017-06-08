(ns ovation.groups
  (:require [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity! unprocessable-entity]]
            [clojure.core.async :refer [chan >!! >! <! go promise-chan]]
            [slingshot.support :refer [get-throwable]]
            [ovation.http :as http]
            [slingshot.slingshot :refer [try+]]))

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
