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

(let [])
(against-background [(request-context/token ..ctx..) => ..auth..
                     ..ctx.. =contains=> {::request-context/auth   ..auth..
                                          ::request-context/routes ..rt..
                                          ::request-context/org    ..org..}
                     (request-context/router ..request..) => ..rt..]
  (facts "About Teams API"
    (facts "teams"
      (let [teams-url (util/join-path [config/TEAMS_SERVER "team_uuids"])
            teams     ["uuid1" "uuid2"]
            authz-ch  (async/promise-chan)
            _         (async/go (async/>! authz-ch {}))]
        (with-fake-http [{:url teams-url :method :get} {:status 200
                                                        :body   (util/to-json {:team_uuids teams})}]

          (fact "calls /teams"
            @(teams/get-teams ..apikey..) => {:team_uuids teams}))))

    (facts "get-team* "
      (against-background []
        (let [team-id    (str (util/make-uuid))
              team-url   (util/join-path [config/TEAMS_SERVER "teams" team-id])
              authz-ch   (async/promise-chan)
              _          (async/go (async/>! authz-ch {:teams {team-id {:id   "1"
                                                                        :uuid team-id
                                                                        :role k/MEMBER-ROLE}}}))
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
                                                        :role_name "Admin"
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
                                                        :membership_role_ids [1, 2, 3]}]
                                 :team_groups         [{:id                  1
                                                        :team_id             2
                                                        :group_id            3
                                                        :role_id             4
                                                        :name                "Some group"}]}}
              expected   {:team {:id                  "1"
                                 :type                "Team"
                                 :name                team-id
                                 :uuid                team-id
                                 :roles               []
                                 :permissions         {:update false
                                                       :delete false}
                                 :pending_memberships [{
                                                        :id        "232"
                                                        :role_name "Admin"
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
                                                        :links               {:self "membership-self"}}]
                                 :team_groups         [{:id                  1
                                                        :team_id             2
                                                        :group_id            3
                                                        :role_id             4
                                                        :name                "Some group"}]
                                 :links               {:self        "team-self"
                                                       :memberships "team-memberships"}}}]

          (fact "should return existing team"
            (with-fake-http [team-url {:status 200
                                       :body   (util/to-json rails-team)}]

              (teams/get-team* ..ctx.. team-id) => expected
              (provided
                (request-context/authorization-ch ..ctx..) => authz-ch
                (routes/named-route ..ctx.. :get-team {:id "1" :org ..org..}) => "team-self"
                (routes/named-route ..ctx.. :post-memberships {:id "1" :org ..org..}) => "team-memberships"
                (routes/named-route ..ctx.. :put-membership {:id "1" :mid "3" :org ..org..}) => "membership-self")))

          (fact "should create new team when it doesn't exist yet"
            (with-fake-http [team-url {:status 404}]
              (get-in (teams/get-team* ..ctx.. team-id) [:team :type]) => nil
              (provided
                (request-context/authorization-ch ..ctx..) => authz-ch
                (teams/create-team ..ctx.. team-id) => {:team {:id   ..id..
                                                               :type "Team"}})))

          (fact "should throw! other response codes"
            (with-fake-http [team-url {:status 401}]
              (teams/get-team* ..ctx.. team-id) => (throws ExceptionInfo)
              (provided
                (request-context/authorization-ch ..ctx..) => authz-ch)))))))


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
                             :role  {:id 1}}
            authz-ch        (async/promise-chan)
            _               (async/go (async/>! authz-ch {}))]

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
              (request-context/authorization-ch ..ctx..) => authz-ch
              (routes/named-route ..ctx.. :put-membership {:id team-uuid :mid membership-id :org ..org..}) => membership-url
              (routes/named-route ..ctx.. :get-team {:id team-uuid :org ..org..}) => team-url
              (routes/named-route ..ctx.. :post-memberships {:id team-uuid :org ..org..}) => memberships-url))))))

  (facts "create-team"
    (against-background [..ctx.. =contains=> {::request-context/org ..org..}
                         (routes/named-route ..ctx.. :post-teams {}) => teams-url]
      (let [team-id        (str (util/make-uuid))

            authz-ch       (async/promise-chan)
            _              (async/go (async/>! authz-ch {:teams {team-id {:id   "1"
                                                                          :uuid team-id
                                                                          :role k/ADMIN-ROLE}}}))

            teams-url      (util/join-path [config/TEAMS_SERVER "teams"])
            team-rt        "team-route"
            memberships-rt "memberships-route"
            team           {:team {:id          team-id
                                   :memberships []
                                   :type        "Team"
                                   :permissions {:update false
                                                 :delete false}
                                   :links       {:memberships memberships-rt
                                                 :self        team-rt}}}]
        (fact "creates team"
          (with-fake-http [{:url teams-url :method :post} {:status 201
                                                           :body   (util/to-json team)}]
            (teams/create-team ..ctx.. team-id) => team
            (provided
              (request-context/authorization-ch ..ctx..) => authz-ch
              (routes/named-route ..ctx.. :get-team {:id team-id :org ..org..}) => team-rt
              (routes/named-route ..ctx.. :post-memberships {:id team-id :org ..org..}) => memberships-rt)))

        (fact "throws! responses not 201"
          (with-fake-http [{:url teams-url :method :post} {:status 401}]
            (teams/create-team ..ctx.. team-id) => (throws ExceptionInfo)
            (provided
              (request-context/authorization-ch ..ctx..) => authz-ch))))))

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
            (teams/get-roles* ..ctx..) => {:roles roles})))))

  (facts "team-permissions"
    (fact "for Member"
      (teams/team-permissions {..team.. {:role k/MEMBER-ROLE}} ..team..) => {:update false
                                                                             :delete false})
    (fact "for Currator"
      (teams/team-permissions {..team.. {:role k/CURATOR-ROLE}} ..team..) => {:update false
                                                                              :delete false})
    (fact "for Admin"
      (teams/team-permissions {..team.. {:role k/ADMIN-ROLE}} ..team..) => {:update true
                                                                            :delete true}))
  (facts "get-authorizations"
    (fact "gets authorizations"
      (let [org-id             3
            rails-response     {"authorization" {"id"           3,
                                                 "behaviors"    {"organization"                  {"read"   true,
                                                                                                  "update" false,
                                                                                                  "delete" false,}
                                                                 "organization-membership"       {"create" false,
                                                                                                  "read"   false,
                                                                                                  "update" false,
                                                                                                  "delete" false,}
                                                                 "organization-group"            {"create" false,
                                                                                                  "read"   false,
                                                                                                  "update" false,
                                                                                                  "delete" false,}
                                                                 "organization-group-membership" {"create" false,
                                                                                                  "read"   false,
                                                                                                  "update" false,
                                                                                                  "delete" false,}
                                                                 "project"                       {"create" true,
                                                                                                  "read"   true,
                                                                                                  "update" true,
                                                                                                  "delete" false,}
                                                                 "research-subscription"         {"create" false,
                                                                                                  "read"   false,
                                                                                                  "update" false,
                                                                                                  "delete" false},}
                                                 "organization" {"id"   org-id,
                                                                 "role" "Member",}
                                                 "teams"        [{"id"   5,
                                                                  "uuid" "3202f460-f160-0130-9ca0-22000aec9fab",}
                                                                 {"id"   20,
                                                                  "uuid" "52b57ad0-0377-0132-cef0-22000a7bab2e",
                                                                  "role" "Ski instructor",}
                                                                 {"id"   67,
                                                                  "uuid" "9a16334e-5439-4989-9282-cec484b00f1d",
                                                                  "role" "Manager",}
                                                                 {"id"   69,
                                                                  "uuid" "9ba5f421-af1f-40f1-b9cb-dbdfd7a8ecc8",
                                                                  "role" "Manager",}
                                                                 {"id"   75,
                                                                  "uuid" "793ee624-b034-496b-bd2e-df03eb015c6d",
                                                                  "role" "Manager",}
                                                                 {"id"   66,
                                                                  "uuid" "6c3b5b2f-0921-4d9d-bbd3-b17a81d85b95",
                                                                  "role" "Admin"}]}}
            url                "base-url"
            authorizations-url (util/join-path [url "authorizations" ..org..])
            expected           (walk/keywordize-keys rails-response)]





        (with-fake-http [{:url authorizations-url :method :get} {:status 200
                                                                 :body   (util/to-json rails-response)}]
          (let [ch (async/chan)]
            (authz/get-authorizations ..ctx.. url ch)
            (<?? ch) => (:authorization expected)))))))



