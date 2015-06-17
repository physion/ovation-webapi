(ns ovation.annotations
  (:require [ovation.version :refer [version-path]]
            [ovation.couch :as couch]
            [ovation.auth :as auth]
            [ovation.core :as core]
            [ovation.links :as links]))


(def ANNOTATIONS_VIEW "annotation_docs")

;; READ
(defn get-annotations
  [auth ids annotation-type]
  (let [db (couch/db auth)
        opts {:keys         (vec (map #(vec [% annotation-type]) ids))
              :include_docs true
              :reduce       false}
        annotations (couch/get-view db ANNOTATIONS_VIEW opts)
        annotations-by-entity (group-by #(keyword (:entity %)) annotations)
        grouped-annotations (map (fn [[entity annotations]]
                                   [entity (group-by #(keyword (:user %)) annotations)]) annotations-by-entity)]

    (into {} grouped-annotations)))


;; WRITE
(defn- make-annotation
  [user-id entity t record]

  (let [entity-id (:_id entity)]
    {:user            user-id
     :entity          entity-id
     :annotation_type t
     :annotation      record
     :type            core/ANNOTATION-TYPE
     :links           {:_collaboration_roots (links/collaboration-roots entity)}}))

(defn create-annotations
  [auth ids annotation-type records]
  (let [auth-user-id (auth/authenticated-user-id auth)
        entities (core/get-entities auth ids)
        docs (flatten (map (fn [entity]
                            (map #(make-annotation auth-user-id entity annotation-type %) records))
                        entities))]

    (core/create-values auth docs)))

(defn delete-annotations
  [auth annotation-ids]
  (core/delete-values auth annotation-ids))
