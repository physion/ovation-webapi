(ns ovation.annotations
  (:require [ovation.version :refer [version-path]]
            [ovation.couch :as couch]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.links :as links]
            [ovation.constants :as k]
            [ovation.util :as util]
            [ring.util.http-response :refer [unprocessable-entity! forbidden!]]))


;; READ
(defn get-annotations
  [auth ids annotation-type]
  (let [db (couch/db auth)
        opts {:keys         (vec (map #(vec [% annotation-type]) ids))
              :include_docs true
              :reduce       false}]

    (couch/get-view auth db k/ANNOTATIONS-VIEW opts)))


;; WRITE
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
    (core/create-values auth routes docs)))

(defn delete-annotations
  [auth annotation-ids routes]
  (core/delete-values auth annotation-ids routes))

(defn update-annotations
  [auth rt annotations]

  (when-not (every? (fn [doc] (= (str (auth/authenticated-user-id auth)) (str (:user doc)))) annotations)
    (forbidden! "Update of an other user's annotations is forbidden"))

  (when-not (every? #{k/NOTES} (map :annotation_type annotations))
    (unprocessable-entity! (str "Cannot update non-Note Annotations")))

  (let [existing (util/into-id-map (core/get-values auth (map :_id annotations) :routes rt))
        updates  (map (fn [update]
                        (if-let [current (get existing (:_id update))]
                          (assoc current :annotation (:annotation update))
                          update)) annotations)
        time     (util/iso-short-now)
        edited   (map #(assoc % :edited_at time) updates)]

    (core/update-values auth rt edited)))




