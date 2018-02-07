(ns ovation.annotations
  (:require [ovation.version :refer [version-path]]
            [ovation.db.notes :as notes]
            [ovation.db.properties :as properties]
            [ovation.db.tags :as tags]
            [ovation.db.timeline_events :as timeline_events]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.links :as links]
            [ovation.util :as util]
            [ovation.html :as html]
            [clojure.tools.logging :as logging]
            [ovation.transform.read :as read]
            [ovation.request-context :as request-context]
            [ring.util.http-response :refer [unprocessable-entity! forbidden!]]
            [ovation.constants :as c]
            [ovation.config :as config]
            [ovation.transform.read :as tr]))


;; READ
(defn get-annotations
  [ctx db ids annotation-type]
  (let [{org-id ::request-context/org
         auth ::request-context/identity} ctx
        teams (auth/authenticated-teams auth)
        user-id (auth/authenticated-user-id auth)
        args {:ids ids
              :owner_id user-id
              :team_uuids teams
              :organization_id org-id}]
      (-> (condp = annotation-type
            c/NOTES           (notes/find-all-by-entity-uuid db args)
            c/PROPERTIES      (properties/find-all-by-entity-uuid db args)
            c/TAGS            (tags/find-all-by-entity-uuid db args)
            c/TIMELINE_EVENTS (timeline_events/find-all-by-entity-uuid db args))
        (tr/values-from-db ctx))))


;; WRITE
(defn sanitized-note-text
  [record]
  (html/escape-html (get-in record [:annotation :text])))


(defn mentions
  "Finds all notified users in note record"
  [note]
  (let [text (sanitized-note-text note)
        matches (re-seq #"\{\{user-mention uuid=([^}]+)\}\}([^{]*)\{\{/user-mention\}\}" text)]
    (map (fn [match] {:uuid (second match)
                      :name (last match)}) matches)))


(defn entity-uri
  [entity]
  (let [tp (util/entity-type-name entity)
        id (:_id entity)
        path (condp = (:type entity)
               c/PROJECT-TYPE id
               (util/join-path [(first (get-in entity [:links :_collaboration_roots])) id]))]
    (str tp "://" path)))


(defn mention-notification-body
  [org-id user-id entity note-id text]

  {:user_id user-id
   :organization_id org-id
   :url (util/join-path [(entity-uri entity) note-id])
   :notification_type c/MENTION_NOTIFICATION
   :body text})


(defn send-mention-notification
  [ctx user-id entity note-id text]
  (let [{auth ::request-context/identity
         org  ::request-context/org} ctx
        body    (mention-notification-body org user-id entity note-id text)
        options {:body    (util/write-json-body body)
                 :headers {"Content-Type" "application/json"
                           "Authorization" (auth/make-bearer auth)}}
        url     (util/join-path [config/NOTIFICATIONS_SERVER "api" "common" "v1" "notifications"])]
    (logging/info (str "Sending mention notification: " user-id))
    (let [resp (org.httpkit.client/post url options)]
      resp)))


(defn notify
  [ctx entity record]
  (if (and (= c/ANNOTATION-TYPE (:type record)) (= c/NOTES (:annotation_type record)))
    (let [text (sanitized-note-text record)
          user-ids (map :uuid (mentions record))]
      (dorun (map (fn [u] (send-mention-notification ctx u entity (:_id record) text)) user-ids))
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
     :type            c/ANNOTATION-TYPE
     :links           {:_collaboration_roots roots}}))

(defn create-annotations
  [ctx db ids annotation-type records]
  (let [{auth ::request-context/identity} ctx
        auth-user-uuid (auth/authenticated-user-uuid auth)
        entities (core/get-entities ctx db ids)
        entity-map (into {} (map (fn [entity] [(:_id entity) entity]) entities))
        docs (doall (flatten (map (fn [entity]
                                    (map #(make-annotation auth-user-uuid entity annotation-type %) records))
                               entities)))]
    (map (fn [doc] (notify ctx (get entity-map (:entity doc)) doc)) (core/create-values ctx db docs))))

(defn delete-annotations
  [ctx db annotation-ids]
  (core/delete-values ctx db annotation-ids))

(defn update-annotation
  [ctx db id annotation]

  (let [{auth ::request-context/identity} ctx
        existing (first (core/get-values ctx db [id]))]

    (when-not (= (str (auth/authenticated-user-uuid auth)) (str (:user existing)))
      (forbidden! "Update of an other user's annotations is forbidden"))

    (when-not (#{c/NOTES} (:annotation_type existing))
      (unprocessable-entity! (str "Cannot update non-Note Annotations")))

    (let [entity (first (core/get-entities ctx db [(:entity existing)]))
          time     (util/iso-now)
          update  (-> existing
                    (assoc :annotation annotation)
                    (assoc :edited_at time))
          _ (core/update-values ctx db [update])
          value (first (core/get-values ctx db [id]))]
      (notify ctx entity value)
      value)))

