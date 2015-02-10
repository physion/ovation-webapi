(ns ovation.dao
  (:import (com.sun.xml.internal.bind.v2.model.core ID)
           (clojure.java.api Clojure))
  (:require [ovation.interop :as interop]
            [ovation.annotations :as annotations]
            [ovation.util :refer [parse-uuid ctx]]
            [ovation.version :refer [version-path]]
            [ovation.util :as util]
            [com.climate.newrelic.trace :refer [defn-traced]]))

(defn-traced get-entity
  "Gets a single entity by ID (uuid string)"
  [api-key id]
  (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id))))


(defn-traced remove-private-links
  "Removes private links (e.g. _collaboration_roots) from the dto.links"
  [dto]
  (if-let [links (:links dto)]
    (let [hidden-links (filter #(re-matches #"_.+" (name %)) (keys links))
          cleaned (apply dissoc links hidden-links)]
      (assoc-in dto [:links] cleaned))
    dto))

(defn-traced add-self-link
  [link dto]
  (assoc-in dto [:links :self] link))

(defn-traced get-entity-annotations
  "Gets the :annotations map for the entity with ID (uuid string)"
  [api-key id]
  (.getAnnotations (get-entity api-key id)))

(defn-traced entity-to-dto
  " Clojure wrapper for entity.toMap()"
  [entity]
  (interop/clojurify (.toMap entity)))

(defn-traced entity-single-link
  "Return a single link from an id and relationship name"
  [id rel]
  (if (= (name rel) "self")
    (clojure.string/join ["/api" version-path "/entities/" id])
    (clojure.string/join ["/api" version-path "/entities/" id "/links/" (name rel)])))

(defn-traced links-to-rel-path
  "Augment an entity dto with the links.self reference"
  [dto]
  (let [add_self (merge-with conj dto {:links {:self ""}})
        new_links_map (into {} (map (fn [x] [(first x) (entity-single-link (:_id dto) (first x))]) (:links add_self)))]
    (assoc-in add_self [:links] new_links_map)))


(defn- get-username
  "Gets the username for a User entity"
  [user]
  (.getUsername user))

(defn-traced username-from-user-uri
  "Gets the username for a user's URI"
  [api-key user-uri]
  (->> user-uri
    (util/get-entity-id)
    (get-entity api-key)
    (get-username)))

(defn- replace-root-uri-keys-with-usernames
  [api-key type-annotations]
  (clojure.set/rename-keys type-annotations
    (into {} (for [[k v] type-annotations] [k (username-from-user-uri api-key k)]))))

(defn-traced replace-uri-keys-with-usernames
  "Replaces user URI keys in the annotations map (i.e. {:tags => {:uri {...}}}) with user names"
  [api-key annotations]
  (into {} (for [[annotation-type type-annotations] annotations] [annotation-type (replace-root-uri-keys-with-usernames api-key type-annotations)])))

(defn-traced replace-annotation-keys
  "Replaces user URI keys in the :annotations map (i.e. {:tags => {:uri {...}}}) with user names"
  [api-key dto]
  (assoc-in dto [:annotations] (replace-uri-keys-with-usernames api-key (:annotations dto))))


(defn-traced dissoc-annotations
  "Removes :annotations from the DTO"
  [dto]
  (dissoc dto :annotations))

(defn-traced convert-entity-to-map
  "Converts an entity to a map suitable for response (e.g. adds additional links=>self)"
  [api-key entity]
  (->> entity
    (entity-to-dto)
    (remove-private-links)
    (links-to-rel-path)
    (annotations/add-annotation-links)                      ;; NB must come after links-to-rel-path
    (dissoc-annotations)))

(defn-traced into-seq
  "Converts a seq of entities into an array of Maps"
  [api-key entity_seq]
  (doall (map (partial convert-entity-to-map api-key) entity_seq))) ;;TODO do we need the doall?
