(ns ovation.pubsub
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as logging]
            [clojure.core.async :refer [go >! >!! chan]]
            [ovation.util :as util])
  (:import (com.google.pubsub.v1 TopicName PubsubMessage)
           (com.google.cloud.pubsub.spi.v1 Publisher)
           (com.google.protobuf ByteString)
           (com.google.api.core ApiFutures ApiFutureCallback)))


(defn make-publisher
  [project-id topic-name]
  (let [tn (TopicName/create project-id topic-name)]
    (doto (Publisher/defaultBuilder tn)
      (.build))))

(defn future-to-ch
  [ch]
  (reify ApiFutureCallback
    (onSuccess [_ result]
      (go (>! ch result)))
    (onFailure [_ throwable]
      (go (>! ch throwable)))))

(defn make-msg
  [message]
  (let [data (ByteString/copyFromUtf8 (util/to-json message))]
    (-> (PubsubMessage/newBuilder) (.setData data) (.build))))

(defn send-msg
  [publisher msg]
  (.publish publisher msg))

(defn add-api-callback
  [future callback]
  (ApiFutures/addCallback future callback))

(defn publish-message
  [publisher message & {:keys [msg-ch] :or {msg-ch (chan)}}]

  (let [msg           (make-msg message)
        msg-id-future (send-msg publisher msg)]

    (add-api-callback msg-id-future (future-to-ch msg-ch))
    msg-ch))

(defprotocol TopicPublisher
  "Publish messages via PubSub"
  (publish [this topic msg result-ch]))

(defrecord PubSub [project-id]
  component/Lifecycle

  (start [this]
    (logging/info "Starting PubSub")
    (assoc this :publishers {}))

  (stop [this]
    (logging/info "Stopping PubSub")
    (if-let [publishers (:publishers this)]
      ;; Shut down all publishers
      (map #(.shutdown %) (vals publishers)))
    (assoc this :publishers nil))

  TopicPublisher
  (publish [this topic msg result-ch]
    (when-not (get-in this [:publishers topic])
      (let [publisher (make-publisher (:project-id this) topic)]
        (update-in this [:publishers] assoc topic publisher)))

    (if-let [publisher (get-in this [:publishers topic])]
      (publish-message publisher msg result-ch))))

(defn new-pubsub [project-id topic]
  (map->PubSub {:project-id project-id
                :topic      topic}))
