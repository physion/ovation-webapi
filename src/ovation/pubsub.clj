(ns ovation.pubsub
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as logging]
            [clojure.core.async :refer [go >! >!! chan close!]]
            [ovation.util :as util])
  (:import (com.google.pubsub.v1 TopicName PubsubMessage)
           (com.google.cloud.pubsub.spi.v1 Publisher)
           (com.google.protobuf ByteString)
           (com.google.api.core ApiFutures ApiFutureCallback)
           (java.io IOException)))


(defn make-publisher
  [project-id topic-name]
  (try
    (let [tn (TopicName/create project-id (name topic-name))]
      (-> (Publisher/defaultBuilder tn)
        (.build)))
    (catch IOException ex
      (logging/error ex "Unable to load Google Cloud Credentials"))))

(defn future-to-ch
  [ch & {:keys [close?] :or {close? false}}]
  (reify ApiFutureCallback
    (onSuccess [_ result]
      (go
        (logging/info "Success publishing to pubsub")
        (>! ch result)
        (if close?
          (close! ch))))
    (onFailure [_ throwable]
      (logging/error throwable "Error publishing to pubsub:" (.getMessage throwable))
      (go
        (>! ch throwable)
        (if close?
          (close! ch))))))

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
  [publisher message & {:keys [channel close?] :or {channel (chan)
                                                    close?  false}}]

  (let [msg           (make-msg message)
        msg-id-future (send-msg publisher msg)]
    (logging/debug "Message sent" msg)
    (add-api-callback msg-id-future (future-to-ch channel :close? close?))
    channel))

(defprotocol Topics
  "Publish messages via PubSub"
  (publish [this topic msg ch])
  (shutdown [this]))

(defrecord TopicPublisher [topics project-id]
  Topics
  (publish [this topic msg ch]
    (logging/info "Publishing message" (str msg) "to" topic)
    (let [publisher (make-publisher (:project-id this) topic)]
      (publish-message publisher msg :channel ch)))

  (shutdown [this]
    (if-let [publishers (:topics this)]
      ;; Shut down all publishers
      (map #(.shutdown %) (vals publishers)))))


(defrecord PubSub [project-id]
  component/Lifecycle

  (start [this]
    (logging/info "Starting PubSub")
    (assoc this :publisher (map->TopicPublisher {:topics {}
                                                 :project-id (:project-id this)})))

  (stop [this]
    (logging/info "Stopping PubSub")
    (if-let [publisher (:publisher this)]
      (shutdown publisher))
    (assoc this :publisher nil)))

(defn new-pubsub [project-id]
  (map->PubSub {:project-id project-id}))
