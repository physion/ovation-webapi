(ns ovation.annotations
  (:require [ovation.version :refer [version-path]]
            [ovation.couch :as couch]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.links :as links]
            [ovation.constants :as k]
            [ovation.util :as util]
            [ovation.html :as html]
            [clojure.tools.logging :as logging]
            [ovation.transform.read :as read]
            [ring.util.http-response :refer [unprocessable-entity! forbidden!]]
            [ovation.constants :as c]
            [ovation.config :as config]))


;; READ
(defn get-annotations
  [auth db ids annotation-type routes]
  (let [opts {:keys         (vec (map #(vec [% annotation-type]) ids))
              :include_docs true
              :reduce       false}]

    (read/values-from-couch (couch/get-view auth db k/ANNOTATIONS-VIEW opts) auth routes)))


;; WRITE
(defn note-text
  [record]
  (html/escape-html (get-in record [:annotation :text])))

(defn mentions
  "Finds all notified users in note record"
  [note]
  (let [text (note-text note)
        matches (re-seq #"\{\{user-mention uuid=([^}]+)\}\}([^{]*)\{\{/user-mention\}\}" text)]
    (map (fn [match] {:uuid (second match)
                      :name (last match)}) matches)))


(defn entity-uri
  [entity]
  (let [tp (util/entity-type-name entity)
        id (:_id entity)
        path (condp = (:type entity)
               k/PROJECT-TYPE id
               (util/join-path [(first (get-in entity [:links :_collaboration_roots])) id]))]
    (str tp "://" path)))


(defn mention-notification-body
  [user-id entity note-id text]

  {:user_id user-id
   :url (util/join-path [(entity-uri entity) note-id])
   :notification_type k/MENTION_NOTIFICATION
   :body text})


(defn send-mention-notification
  [auth user-id entity note-id text]
  (let [body    (mention-notification-body user-id entity note-id text)
        options {:body    (util/write-json-body body)
                 :headers {"Content-Type" "application/json"
                           "Authorization" (auth/make-bearer auth)}}
        url     (util/join-path [config/NOTIFICATIONS_SERVER "api" "common" "v1" "notifications"])]
    (logging/info (str "Sending mention notification: " user-id))
    (let [resp (org.httpkit.client/post url options)]
      resp)))


(defn notify
  [auth entity record]
  (if (and (= c/ANNOTATION-TYPE (:type record)) (= c/NOTES (:annotation_type record)))
    (let [text (note-text record)
          user-ids (map :uuid (mentions record))]
      (dorun (map (fn [u] (send-mention-notification auth u entity (:_id record) text)) user-ids))
      record)
    record))

(defn- make-annotation
  [user-id entity t record]

  (let [entity-id (:_id entity)
        entity-collaboration-roots (links/collaboration-roots entity)
        roots (if (or  (nil? entity-collaboration-roots) (empty? entity-collaboration-roots))
                [entity-id]
                entity-collaboration-roots)]
    {:_id             (util/make-uuid)
     :user            user-id
     :entity          entity-id
     :annotation_type t
     :annotation      record
     :type            k/ANNOTATION-TYPE
     :links           {:_collaboration_roots roots}}))

(defn create-annotations
  [auth db routes ids annotation-type records]
  (let [auth-user-id (auth/authenticated-user-id auth)
        entities (core/get-entities auth db ids routes)
        entity-map (into {} (map (fn [entity] [(:_id entity) entity]) entities))
        docs (doall (flatten (map (fn [entity]
                                    (map #(make-annotation auth-user-id entity annotation-type %) records))
                               entities)))]
    (map (fn [doc] (notify auth (get entity-map (:entity doc)) doc)) (core/create-values auth db routes docs))))

(defn delete-annotations
  [auth db annotation-ids routes]
  (core/delete-values auth db annotation-ids routes))

(defn update-annotation
  [auth db rt id annotation]

  (let [existing (first (core/get-values auth [id] :routes rt))]

    (when-not (= (str (auth/authenticated-user-id auth)) (str (:user existing)))
      (forbidden! "Update of an other user's annotations is forbidden"))

    (when-not (#{k/NOTES} (:annotation_type existing))
      (unprocessable-entity! (str "Cannot update non-Note Annotations")))

    (let [entity (first (core/get-entities auth db [(:entity existing)] rt))
          time     (util/iso-short-now)
          update  (-> existing
                    (assoc :annotation annotation)
                    (assoc :edited_at time))]

      (first (map (fn [doc] (notify auth entity doc)) (core/update-values auth db rt [update]))))))




