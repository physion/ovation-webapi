(ns ovation.test.pubsub
  (:use midje.sweet)
  (:require [ovation.pubsub :as pubsub]
            [ovation.util :refer [<??]]
            [clojure.core.async :as async]))

(facts "About pubsub"
  (facts "publish-message"
    (fact "publishes to a topic"
      (let [message {:foo "bar"}]
        (let [ch (async/chan)]
          (pubsub/publish-message ..pub.. message)
          (.onSuccess (pubsub/future-to-ch ch) message)
          (first (async/alts!! [(async/timeout 100) ch]))) => message
        (provided
          (pubsub/make-msg message) => ..msg..
          (pubsub/send-msg ..pub.. ..msg..) => ..future..
          (pubsub/add-api-callback ..future.. anything) => nil))))
  (facts "future-to-ch"
    (fact "propagates exceptions"
      (let [ch (async/chan)]
        (.onFailure (pubsub/future-to-ch ch) (Exception. "Boom"))
        (<?? ch)) => (throws Throwable))))
