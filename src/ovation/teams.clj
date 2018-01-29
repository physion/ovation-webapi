(ns ovation.teams
  (:require [ovation.routes :as routes]
            [ovation.util :as util :refer [<??]]
            [ovation.config :as config]
            [ovation.constants :as c]
            [ovation.db.membership_roles :as membership_roles]
            [ovation.db.memberships :as memberships]
            [ovation.db.projects :as projects]
            [ovation.db.roles :as roles]
            [ovation.db.teams :as db.teams]
            [ovation.transform.read :as transform.read]
            [org.httpkit.client :as httpkit.client]
            [clojure.tools.logging :as logging]
            [ring.util.http-response :refer [throw! bad-request! not-found! unprocessable-entity! unprocessable-entity]]
            [ovation.constants :as c]
            [clojure.core.async :refer [chan >!! >! <! go promise-chan]]
            [slingshot.support :refer [get-throwable]]
            [ring.util.http-predicates :as hp]
            [ovation.http :as http]
            [slingshot.slingshot :refer [try+]]
            [ovation.request-context :as request-context]
            [ovation.auth :as auth]))

(def TEAMS "teams")

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
      c/ADMIN-ROLE {:update true
                    :delete true}
      c/CURATOR-ROLE {:update false
                      :delete false}
      c/MEMBER-ROLE {:update false
                     :delete false}
      {:update false
       :delete false})))

(defn make-read-team-tf
  [ctx]
  (let [authorization-ch (request-context/authorization-ch ctx)]

    (fn [team]
      (let [team-id            (:name team)
            memberships        (:memberships team)
            linked-memberships (map #(assoc-in % [:links :self] (routes/named-route ctx :put-membership {:id team-id :mid (:id %) :org (:ovation.request-context/org ctx)})) memberships)
            teams              (if authorization-ch
                                 (get-in (<?? authorization-ch) [:teams])
                                 (do
                                   (logging/error "make-read-team-tf: authorization channel is nil")
                                   {}))]

        (-> team
          (assoc :type c/TEAM-TYPE)
          (dissoc :project)
          (dissoc :organization)
          (dissoc :project_id)
          (dissoc :organization_id)
          (assoc :memberships linked-memberships)
          (assoc :permissions (team-permissions teams (:uuid team)))
          (assoc :links {:self        (routes/named-route ctx :get-team {:id team-id :org (:ovation.request-context/org ctx)})
                         :memberships (routes/named-route ctx :post-memberships {:id team-id :org (:ovation.request-context/org ctx)})}))))))

(defn -create-team
  [ctx db team-uuid]
  (let [{auth ::request-context/identity
         org-id ::request-context/org} ctx
        owner (auth/authenticated-user-id auth)
        record {:owner_id owner
                :name (str team-uuid)
                :uuid (str team-uuid)
                :entity_id org-id
                :entity_type c/ORGANIZATION-TYPE
                :created-at (util/iso-now)
                :updated-at (util/iso-now)}
        _ (logging/info (str "-create-team" record))
        result (db.teams/create db record)]
    (-> record
      (assoc :id (:generated_key result)))))

(defn -create-membership
  [ctx db team-record])

(defn -create-membership
  [ctx db team-record user-id]
  (logging/info "-create-membership for " team-record user-id)
  (let [record {:team_id (:id team-record)
                :user_id user-id
                :created-at (util/iso-now)
                :updated-at (util/iso-now)}
        _ (logging/info "-create-membership record " record)
        result (memberships/create db record)]
    (-> record
      (assoc :id (:generated_key result)))))

(defn -find-or-create-membership
  [ctx db team-record user-id]
  (or
    (memberships/find-by db {:team_id (:id team-record)
                             :user_id user-id})
    (-create-membership ctx db team-record user-id)))

(defn -create-membership-role
  [ctx db team-record membership-record role-record]
  (let [record {:membership_id (:id membership-record)
                :team_id (:id team-record)
                :role_id (:id role-record)}
        result (membership_roles/create db record)]
    (-> record
      (assoc :id (:generated_key result)))))

(defn -find-or-create-membership-role
  [ctx db team-record membership-record role-record]
  (or
    (membership_roles/find-by db {:team_id (:id team-record)
                                  :membership_id (:id membership-record)
                                  :role_id (:id role-record)})
    (-create-membership-role ctx db team-record membership-record role-record)))

(defn create-team
  [ctx db team-uuid]
  (logging/info (str "Creating Team for " team-uuid))
  (let [{auth ::request-context/identity} ctx
        user-id (auth/authenticated-user-id auth)
        team (-create-team ctx db team-uuid)
        membership (-create-membership ctx db team user-id)
        role (roles/find-admin-role db)
        membership-role (-create-membership-role ctx db team membership role)]
    team))

(defn get-team
  [ctx team-id ch]
  (http/show-resource ctx config/TEAMS_SERVER "teams" team-id ch
    :response-key :team
    :make-tf make-read-team-tf))

(defn get-project
  [ctx db id]
  (let [{org-id ::request-context/org, auth ::request-context/identity} ctx
        teams (auth/authenticated-teams auth)
        user (auth/authenticated-user-id auth)]
    (-> (projects/find-all-by-uuid {:ids [id]
                                    :archived false
                                    :organization_id org-id
                                    :team_uuids teams
                                    :owner_id user})
      (transform.read/entities-from-db ctx)
      (first))))

(defn get-team*
  [ctx db team-id]

  (let [ch      (chan)
        team-ch (chan)]
    (go
      (get-team ctx team-id team-ch)
      (let [team (<! team-ch)]
        (if (util/exception? team)
          (if (hp/not-found? (:response team))
            (if-let [project (get-project ctx db team-id)]
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

  (let [ch (chan)
        role (:role membership)
        body {:membership {:role_id (:id role)}} ]
    (http/update-resource ctx config/TEAMS_SERVER "memberships" body membership-id ch
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
                :response-key :membership
                :make-tf (make-read-membership-tf team-uuid)))))))

    {:membership (<?? ch)}))


(defn delete-membership*
  [ctx membership-id]
  (let [ch (chan)]
    (http/destroy-resource ctx config/TEAMS_SERVER "memberships" membership-id ch)
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
