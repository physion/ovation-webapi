(ns ovation.test.authz
  (:use midje.sweet)
  (:require [ovation.authz :as authz]
            [clojure.core.async :as async]
            [ovation.util :as util]
            [ovation.teams :as teams]))


(facts "About AuthzService"

  (fact "proxies authorizations"
    (let [z (authz/new-authz-service ..v1.. ..v2..)]
      (authz/get-authorizations z ..ctx..) => ..result..
      (provided
        (async/promise-chan) => ..ch..
        (teams/get-authorizations ..ctx.. ..v2.. ..ch..) => ..nothing..
        (util/<?? ..ch..) => ..result..))))
