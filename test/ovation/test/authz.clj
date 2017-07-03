(ns ovation.test.authz
  (:use midje.sweet)
  (:require [ovation.authz :as authz]
            [ovation.util :as util]
            [ovation.request-context :as request-context]
            [org.httpkit.fake :refer [with-fake-http]]
            [clojure.core.async :as async :refer [go >!]]
            [ovation.groups :as groups]))


(facts "About AuthzService"

  (fact "proxies authorizations"
    (let [z (authz/new-authz-service ..url..)]
      (authz/get-authorization z ..ctx..) => {:authorization ..result..}
      (provided
        ..ctx.. =contains=> {::request-context/org ..org..}
        (async/promise-chan) => ..ch..
        (authz/get-authorizations ..ctx.. ..url.. ..ch..) => ..go..
        (util/<?? ..ch..) => ..result..))))

