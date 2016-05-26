(ns ovation.annotations
  (:require [ovation.version :refer [version-path]]
            [ovation.couch :as couch]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.links :as links]
            [ovation.constants :as k]
            [ovation.util :as util]
            [ovation.html :as html]
            [ring.util.http-response :refer [unprocessable-entity! forbidden!]]
            [ovation.constants :as c]
            [ovation.config :as config]))


;; READ
(defn get-annotations
  [auth ids annotation-type]
  (let [db (couch/db auth)
        opts {:keys         (vec (map #(vec [% annotation-type]) ids))
              :include_docs true
              :reduce       false}]

    (couch/get-view auth db k/ANNOTATIONS-VIEW opts)))


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


(defn mention-notification-body
  [user-id entity-id note-id text]

  {:user_id user-id
   :url (util/join-path [entity-id note-id])
   :notification_type k/MENTION_NOTIFICATION
   :body text})


(defn send-mention-notification
  [auth user-id entity-id note-id text]
  (let [body    (mention-notification-body user-id entity-id note-id text)
        options {:body    (util/write-json-body body)
                 :headers {"Content-Type" "application/json"
                           "Authorization" (auth/make-bearer auth)}}
        url     (util/join-path [config/NOTIFICATIONS_SERVER "api" "common" "v1" "notifications"])]
    (ovation.logging/info (str "Sending mention notification: " user-id))
    (let [resp (org.httpkit.client/post url options)]
      resp)))


(defn notify
  [auth record]
  (if (and (= c/ANNOTATION-TYPE (:type record)) (= c/NOTES (:annotation_type record)))
    (let [text (note-text record)
          user-ids (map :uuid (mentions record))]
      (doall (map (fn [u] (send-mention-notification auth u (:entity record) (:_id record) text)) user-ids))
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
  [auth routes ids annotation-type records]
  (let [auth-user-id (auth/authenticated-user-id auth)
        entities (core/get-entities auth ids routes)
        docs (doall (flatten (map (fn [entity]
                                    (map #(make-annotation auth-user-id entity annotation-type %) records))
                               entities)))]
    (map (fn [doc] (notify auth doc)) (core/create-values auth routes docs))))

(defn delete-annotations
  [auth annotation-ids routes]
  (core/delete-values auth annotation-ids routes))

(defn update-annotation
  [auth rt id annotation]

  (let [existing (first (core/get-values auth [id] :routes rt))]

    (when-not (= (str (auth/authenticated-user-id auth)) (str (:user existing)))
      (forbidden! "Update of an other user's annotations is forbidden"))

    (when-not #{k/NOTES} (:annotation_type existing)
      (unprocessable-entity! (str "Cannot update non-Note Annotations")))

    (let [time     (util/iso-short-now)
          update  (-> existing
                    (assoc :annotation annotation)
                    (assoc :edited_at time))]

      (first (map (fn [doc] (notify auth doc)) (core/update-values auth rt [update]))))))




