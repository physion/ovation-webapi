(ns ovation.groups
  (:require [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity! unprocessable-entity]]
            [clojure.core.async :refer [chan >!! >! <! go promise-chan]]
            [slingshot.support :refer [get-throwable]]
            [ovation.http :as http]
            [slingshot.slingshot :refer [try+]]
            [ovation.constants :as k]))

(defn make-read-team-group-tf
  [_]
  (fn [group]
    (-> group
      (select-keys [:id :team_id :organization_group_id :role_id :name])
      (assoc :type k/TEAM-GROUP-TYPE))))

(defn get-team-groups
  [ctx url team-id ch]
  (http/index-resource ctx url k/TEAM-GROUPS ch
    :response-key :team_groups
    :make-tf make-read-team-group-tf
    :query-params {:team_id team-id})

  ch)

(defn create-team-group
  [ctx url new-group ch]
  (http/create-resource ctx url k/TEAM-GROUPS new-group ch
    :response-key :team_group
    :make-tf make-read-team-group-tf)
  ch)

(defn get-team-group
  [ctx url group-id ch]
  (http/show-resource ctx url k/TEAM-GROUPS group-id ch
    :response-key :team_group
    :make-tf make-read-team-group-tf)
  ch)

(defn update-team-group
  [ctx url group-id body ch]
  (http/update-resource ctx url k/TEAM-GROUPS body group-id ch
    :response-key :team_group
    :make-tf make-read-team-group-tf)
  ch)

(defn delete-team-group
  [ctx url group-id ch]
  (http/destroy-resource ctx url k/TEAM-GROUPS group-id ch
    :response-key :team_group)
  ch)
