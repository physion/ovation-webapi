(ns ovation.test.teams
  (:use midje.sweet)
  (:require [ovation.auth :as auth]
            [ovation.teams :as teams]
            [ovation.routes :as routes]
            [org.httpkit.fake :refer [with-fake-http]]
            [ovation.util :as util :refer [<??]]
            [ovation.config :as config]
            [ovation.request-context :as request-context])

  (:import (clojure.lang ExceptionInfo)))

(against-background [(request-context/token ..ctx..) => ..auth..
                     ..ctx.. =contains=> {::request-context/auth   ..auth..
                                          ::request-context/routes ..rt..
                                          ::request-context/org ..org..}
                     (request-context/router ..request..) => ..rt..]
  (facts "About Teams API"
    (facts "teams"
      (let [teams-url (util/join-path [config/TEAMS_SERVER "team_uuids"])
            teams     ["uuid1" "uuid2"]]
        (with-fake-http [{:url teams-url :method :get} {:status 200
                                                        :body   (util/to-json {:team_uuids teams})}]

          (fact "calls /teams"
            @(teams/get-teams ..apikey..) => {:team_uuids teams}))))

    (facts "get-team*"
      (against-background []
        (let [team-id    (str (util/make-uuid))
              team-url   (util/join-path [config/TEAMS_SERVER "teams" team-id])
              rails-team {:team {:id                  "1"
                                 :type                "Team"
                                 :name                team-id
                                 :uuid                team-id
                                 :organization        {}
                                 :project             {}
                                 :roles               []
                                 :pending_memberships [{
                                                        :id        "232"
                                                        :type      "PendingMembership"
                                                        :role_name "Administrator'"
                                                        :email     "newmember@example.com",}
                                                       {
                                                        :id        "2323"
                                                        :type      "PendingMembership"
                                                        :role_name "Member"
                                                        :email     "newmember@example.com"}]

                                 :memberships         [{:id                  "3"
                                                        :team_id             1
                                                        :type                "Membership"
                                                        :added               "2015-02-01"
                                                        :role_id             21
                                                        :user_id             "3"
                                                        :membership_role_ids [1, 2, 3]}]}}
              expected   {:team {:id                  "1"
                                 :type                "Team"
                                 :name                team-id
                                 :uuid                team-id
                                 :roles               []
                                 :pending_memberships [{
                                                        :id        "232"
                                                        :role_name "Administrator'"
                                                        :email     "newmember@example.com"
                                                        :type      "PendingMembership",}
                                                       {
                                                        :id        "2323"
                                                        :role_name "Member"
                                                        :email     "newmember@example.com"
                                                        :type      "PendingMembership"}]

                                 :memberships         [{:id                  "3"
                                                        :team_id             1
                                                        :added               "2015-02-01"
                                                        :role_id             21
                                                        :type                "Membership"
                                                        :user_id             "3"
                                                        :membership_role_ids [1, 2, 3]
                                                        :links               {:self ..membership-url..}}]
                                 :links               {:self        ..self-url..
                                                       :memberships ..membership-url..}}}]

          (fact "should return existing team"
            (with-fake-http [team-url {:status 200
                                       :body   (util/to-json rails-team)}]

              (teams/get-team* ..ctx.. team-id) => expected
              (provided
                (request-context/token ..ctx..) => ..token..
                (routes/named-route ..ctx.. :get-team {:id team-id :org ..org..}) => ..self-url..
                (routes/named-route ..ctx.. :post-memberships {:id team-id :org ..org..}) => ..membership-url..
                (routes/named-route ..ctx.. :put-membership {:id team-id :mid "3" :org ..org..}) => ..membership-url..)))

          (fact "should create new team when it doesn't exist yet"
            (with-fake-http [team-url {:status 404}]
              (get-in (teams/get-team* ..ctx.. team-id) [:team :type]) => "Team"
              (provided
                (teams/create-team ..ctx.. team-id) => {:team {:id ..id..}})))

          (fact "should throw! other response codes"
            (with-fake-http [team-url {:status 401}]
              (teams/get-team* ..ctx.. team-id) => (throws ExceptionInfo))))))


    (facts "post-memberhsip*"
      (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..]
        (let [team-uuid       (str (util/make-uuid))
              team-id         "1"
              team-url        (util/join-path [config/TEAMS_SERVER "teams" team-uuid])
              memberships-url (util/join-path [config/TEAMS_SERVER "memberships"])
              membership-id   "1"
              membership-url  (util/join-path [memberships-url membership-id])
              team            {:team {:id          team-uuid
                                      :memberships []}}
              user-email      "example@example.com"
              membership      {:email user-email
                               :role  {:id 1}}]

          (against-background [(routes/named-route ..ctx.. :put-membership {:id team-uuid :mid membership-id :org ..org..}) => membership-url
                               (routes/named-route ..ctx.. :get-team {:id team-uuid}) => team-url
                               (routes/named-route ..ctx.. :post-memberships {:id team-uuid}) => memberships-url]
            (fact "creates membership for existing team"
              (with-fake-http [team-url {:status 200
                                         :body   (util/to-json team)}
                               {:url memberships-url :method :post} {:status 201
                                                                     :body   (util/to-json {:membership {:id      membership-id
                                                                                                         :team_id team-id
                                                                                                         :email   user-email
                                                                                                         :role_id 1}})}]
                (teams/post-membership* ..ctx.. team-uuid membership) => {:membership {:email   user-email,
                                                                                           :id      "1",
                                                                                           :links   {:self membership-url},
                                                                                           :role_id 1,
                                                                                           :team_id team-id}}
                (provided
                  (teams/get-team* ..ctx.. team-uuid) => {:team {:id team-id}})))))))

    (facts "create-team"
      (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                           ..request.. =contains=> {:identity ..auth..}
                           (request-context/router ..request..) => ..rt..
                           (routes/named-route ..ctx.. :post-teams {}) => teams-url]
        (let [team-id   (str (util/make-uuid))
              teams-url (util/join-path [config/TEAMS_SERVER "teams"])
              team      {:team {:id          team-id
                                :memberships []}}]
          (fact "creates team"
            (with-fake-http [{:url teams-url :method :post} {:status 201
                                                             :body   (util/to-json team)}]
              (teams/create-team ..ctx.. team-id) => team))

          (fact "throws! responses not 201"
            (with-fake-http [{:url teams-url :method :post} {:status 401}]
              (teams/create-team ..ctx.. team-id) => (throws ExceptionInfo))))))

    (facts "put-membership*"
      (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                           ..request.. =contains=> {:identity ..auth..}
                           (request-context/router ..request..) => ..rt..]
        (fact "throws 422 if mid is not specified"
          (teams/put-membership* .ctx.. ..team.. {:id 1 :role {:id ..roleid..}} nil) => (throws ExceptionInfo))))

    (facts "put-pending-membership*"
      (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                           ..request.. =contains=> {:identity ..auth..}
                           (request-context/router ..request..) => ..rt..]
        (fact "throws 422 if mid is not specified"
          (teams/put-pending-membership* ..ctx.. ..team.. {:id 1 :role {:id ..roleid..}} nil) => (throws ExceptionInfo))))

    (facts "get-roles*"
      (against-background [(auth/authenticated-user-id ..auth..) => ..user-id..
                           ..request.. =contains=> {:identity ..auth..}
                           (request-context/router ..request..) => ..rt..]
        (let [roles-url (util/join-path [config/TEAMS_SERVER "roles"])
              roles     [{:id              "2323",
                          :name            "Member",
                          :organization_id 1,
                          :links           {}},
                         {:id              "233",
                          :name            "Currator",
                          :organization_id 1,
                          :links           {}}]]
          (fact "gets organization roles"
            (with-fake-http [{:url roles-url :method :get} {:status 200
                                                            :body   (util/to-json {:roles roles})}]
              (teams/get-roles* ..ctx..) => {:roles roles})))))))

