(ns ovation-rest.dao
  (:require [ovation-rest.interop :as interop]
            [ovation-rest.annotations :as annotations]
            [ovation-rest.util :refer [parse-uuid ctx]]
            [ovation-rest.version :refer [version-path]]
            [ovation-rest.util :as util]))

(defn get-entity
  "Gets a single entity by ID (uuid string)"
  [api-key id]
  (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id))))

(defn entity-to-dto
  "Clojure wrapper for entity.toMap()"
  [entity]
  (interop/clojurify (.toMap entity)))

(defn single-link
  "Return a single link from an id and relationship name"
  [id, rel]
  (if (= (name rel) "self")
    (clojure.string/join ["/api" version-path "/entities/" id])
    (clojure.string/join ["/api" version-path "/entities/" id "/links/" (name rel)])))

(defn links-to-rel-path
  "Augment an entity dto with the links.self reference"
  [dto]
  (let [add_self         (merge-with conj dto {:links {:self ""}})
        new_links_map    (into {} (map (fn [x] [(first x) (single-link (dto :_id) (first x))]) (add_self :links)))]
    (assoc-in add_self [:links] new_links_map)))

(defn convert-entity-to-map
  "Converts an entity to a map suitable for response (e.g. adds additional links=>self)"
  [entity]
  (let [convert-to-dto   (entity-to-dto entity)
        convert-links    (links-to-rel-path convert-to-dto)
        annotation-links (annotations/add-annotation-links convert-links)]
    annotation-links))

(defn into-seq
  "Converts a seq of entities into an array of Maps"
  [entity_seq]
  (doall (map (partial convert-entity-to-map) entity_seq)))


(defn- get-username
  "Gets the username for a User entity"
  [user]
  (.getUsername user))

(defn username-from-user-uri
  "Gets the username for a user's URI"
  [api-key user-uri]
  (let [id (util/get-entity-id user-uri)
        user (get-entity api-key id)]
    (get-username user)))
