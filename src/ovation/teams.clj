(ns ovation.teams
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
            [ovation.request-context :as request-context]
            [ovation.core :as core]
            [ovation.auth :as auth]))



(defn get-teams
  "Gets all teams for authenticated user as a future assoc: @{:user_uuid id :team_uuids [id1 id2] :organization_ids [id1 id2]}"
  [api-token]
  (let [opts {:timeout     10000                            ; ms
              :oauth-token api-token
              :headers     {"Content-Type" "application/json; charset=utf-8"}}
        url  (util/join-path [config/TEAMS_SERVER "team_uuids"])]
    (future (let [resp @(httpkit.client/get url opts)]
              (if (hp/ok? resp)
                (-> resp
                  :body
                  util/from-json))))))


(defn make-read-membership-tf
  [team-uuid]
  (fn
    [ctx]
    (fn [membership]
      (let [org           (::request-context/org ctx)
            self-route    :put-membership
            membership-id (:id membership)]
        (-> membership
          (dissoc :users)
          (dissoc :membership_roles)
          (assoc-in [:links :self] (routes/named-route ctx self-route {:org org :id team-uuid :mid membership-id})))))))

(defn team-permissions
  [teams team-id]
  (let [role (get-in teams [(keyword (str team-id)) :role])]
    (condp = role
      k/ADMIN-ROLE {:update true
                    :delete true}
      k/CURATOR-ROLE {:update false
                      :delete false}
      k/MEMBER-ROLE {:update false
                     :delete false}
      {:update false
       :delete false})))

(defn make-read-team-tf
  [ctx]
  (let [authorization-ch (request-context/authorization-ch ctx)]

    (fn [team]
      (let [team-id            (:id team)
            memberships        (:memberships team)
            linked-memberships (map #(assoc-in % [:links :self] (routes/named-route ctx :put-membership {:id team-id :mid (:id %) :org (:ovation.request-context/org ctx)})) memberships)
            teams              (if authorization-ch
                                 (get-in (<?? authorization-ch) [:teams])
                                 (do
                                   (logging/error "make-read-team-tf: authorization channel is nil")
                                   {}))]

        (-> team
          (assoc :type k/TEAM-TYPE)
          (dissoc :project)
          (dissoc :organization)
          (dissoc :project_id)
          (dissoc :organization_id)
          (assoc :memberships linked-memberships)
          (assoc :permissions (team-permissions teams (:uuid team)))
          (assoc :links {:self        (routes/named-route ctx :get-team {:id team-id :org (:ovation.request-context/org ctx)})
                         :memberships (routes/named-route ctx :post-memberships {:id team-id :org (:ovation.request-context/org ctx)})}))))))

(defn create-team
  [ctx team-uuid]

  (let [ch   (chan)
        org  (::request-context/org ctx)
        body {:team {:uuid            (str team-uuid)
                     :organization_id org}}]

    (logging/info (str "Creating Team for " team-uuid))

    (http/create-resource ctx config/TEAMS_SERVER "teams" body ch
      :response-key :team
      :make-tf make-read-team-tf)

    (let [team (<?? ch)]
      team)))

(defn get-team
  [ctx team-id ch]
  (http/show-resource ctx config/TEAMS_SERVER "teams" team-id ch
    :response-key :team
    :make-tf make-read-team-tf))


(defn get-team*
  [ctx db team-id]

  (let [ch      (chan)
        team-ch (chan)]
    (go
      (get-team ctx team-id team-ch)
      (let [team (<! team-ch)]
        (if (util/exception? team)
          (if (hp/not-found? (:response team))
            (if-let [project (core/get-entity ctx db team-id)]
              (if (auth/can? ctx ::auth/update project)
                (>! ch (create-team ctx team-id)))
              (>! ch team))
            (>! ch team))
          (>! ch team))))
    {:team (<?? ch)}))


(defn put-membership*
  [ctx team-uuid membership membership-id]                  ;; membership is a TeamMembership

  (when (or (nil? (get-in membership [:role :id]))
          (nil? membership-id))
    (unprocessable-entity!))

  (let [ch (chan)]
    (http/update-resource ctx config/TEAMS_SERVER "memberships" {:membership membership} membership-id ch
      :response-key :membership
      :make-tf (make-read-membership-tf team-uuid))

    {:membership (<?? ch)}))


(defn post-membership*
  [ctx team-uuid membership]                                ;; membership is a NewTeamMembership

  (let [ch      (chan)
        team-ch (chan)]
    (go
      (get-team ctx team-uuid team-ch)
      (let [team (<! team-ch)]
        (if (util/exception? team)
          (>! ch team)

          (let [team-id (:id team)
                role    (:role membership)
                email   (:email membership)
                body    {:membership {:team_id team-uuid
                                      :role_id (:id role)
                                      :email   email}}]
            (if (or (nil? team-id) (nil? role) (nil? email))
              (>! ch (unprocessable-entity))

              (http/create-resource ctx config/TEAMS_SERVER "memberships" body ch
                :response-key :membership                   ;;TODO how to handle pending memberships?
                :make-tf (make-read-membership-tf team-uuid)))))))

    {:membership (<?? ch)}))


(defn delete-membership*
  [ctx membership-id]
  (let [ch (chan)]
    (http/destroy-resource ctx config/TEAMS_SERVER "memberships" membership-id ch)
    (<?? ch)))


(defn delete-pending-membership*
  [ctx membership-id]
  (let [ch (chan)]
    (http/destroy-resource ctx config/TEAMS_SERVER "pending_memberships" membership-id ch)
    (<?? ch)))

(defn get-roles*
  [ctx]
  (let [ch        (chan)
        read-role (fn [_]                                   ;;unused ctx
                    (fn [role]
                      (select-keys role [:id :name :organization_id :links])))]
    (http/index-resource ctx config/TEAMS_SERVER "roles" ch
      :response-key :roles
      :make-tf read-role)
    (let [roles (<?? ch)]
      {:roles roles})))
