(ns ovation.test.authz
  (:use midje.sweet)
  (:require [ovation.authz :as authz]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [org.httpkit.fake :refer [with-fake-http]]
            [clojure.core.async :as async]))


(facts "About AuthzService"

  (fact "proxies authorizations"
    (let [z (authz/new-authz-service ..url..)]
      (authz/get-authorization z ..ctx..) => {:authorization ..result..}
      (provided
        ..ctx.. =contains=> {::request-context/org ..org..}
        (async/promise-chan) => ..ch..
        (authz/get-authorizations ..ctx.. ..url.. ..ch..) => ..go..
        (util/<?? ..ch..) => ..result..)))


  (facts "`get-team-group-project-ids`"
    (fact "Gets projects by group->projects UUIDs"
      (let [group-id    1
            rails-group {:team_ids ..ids..}
            authz       (authz/new-authz-service ..url..)]

        (authz/get-team-group-project-ids authz ..ctx.. group-id) => ..ids..
        (provided
          (authz/get-team-group authz ..ctx.. group-id) => {:team-group rails-group})))))

