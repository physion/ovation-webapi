(ns ovation.entity
  (:import (us.physion.ovation.util MultimapUtils)
           (us.physion.ovation.data EntityDao$Views))
  (:require [clojure.walk :refer [stringify-keys]]
            [ovation.dao :refer [get-entity entity-to-dto into-seq]]
            [ovation.util :refer [ctx create-uri parse-uuid]]
            [slingshot.slingshot :refer [try+ throw+]]
            [ovation.context :refer [transaction]]
            [ovation.links :as links]
            [ovation.interop :as interop]
            [ovation.annotations :as annotations]
            [ovation.version :as ver]
            [ovation.dao :as dao]
            [com.ashafa.clutch :as cl]
            [ovation.couch :as couch]))



(defn filter-trashed
  "Removes entity documents with a non-nil trash_info from seq"
  [entities include_trashed]
  (filter #(or include_trashed (nil? (:trash_info %))) entities))

(defn of-type
  "Gets all entities of the given type"
  [auth resource & {:keys [include_trashed] :or {include_trashed false}}]

  (-> (map :doc (cl/with-db (couch/db auth)
                  (cl/get-view couch/design-doc us.physion.ovation.data.EntityDao$Views/ENTITIES_BY_TYPE {:key resource :reduce false :include_docs true})))
    (couch/transform)
    (filter-trashed include_trashed)))


(defn get-entities
  "Gets entities by ID"
  [auth entity-ids & {:keys [include_trashed] :or {include_trashed false}}]

  (-> (map :doc (cl/with-db (couch/db auth)
                  (cl/all-documents {:include_docs true} {:keys entity-ids})))
    (couch/transform)
    (filter-trashed include_trashed)))

(defn create-multimap [m]
  (MultimapUtils/createMultimap m))

(defn -ensure-id
  "Makes sure there's an _id for entity"
  [doc]
  (if (nil? (:_id doc))
    (assoc doc :_id (java.util.UUID/randomUUID))
    doc))

(defn -ensure-api-version
  "Insert API version"
  [doc]
  (assoc doc :api_version ver/schema-version))

(defn -add-owner
  "Ensures that the owner relation is built.

  Returns {:doc doc :additional_docs []}"

  [doc auth]

  ;;TODO we need to return the extra document somehow
  doc)



(defn insert-entity
  "Inserts dto as an entity into the given DataContext"
  [auth raw_dto]

  ;; TODO
  ;; ensure _id
  ;; ensure owner
  ;; ensure links
  ;; collaboration_roots
  (let [dto (-> raw_dto
              (-ensure-id)
              (-ensure-api-version)
              (-add-owner auth))]

    (cl/with-db (couch/db auth)
      (cl/bulk-update '(dto)))))

(defn create-entity
  "Creates a new Entity from a DTO map"
  [auth api-key new-dto]

  (let [links (:links new-dto)
        named-links (:named_links new-dto)
        dto (stringify-keys (dissoc new-dto :links :named_links))]

    (let [entity (insert-entity auth dto)]
      ;; For all links, add the link
      (when links
        (doseq [[rel rel-links] links]
          (doseq [link rel-links]
            (links/add-link entity (name rel) (create-uri (:target_id link)) :inverse (:inverse_rel link)))))

      ;; For all named links, add the named link
      (when named-links
        (doseq [[rel names] named-links]
          (doseq [[named rel-links] names]
            (doseq [link rel-links]
              (links/add-named-link entity (name rel) (name named) (create-uri (:target_id link)) :inverse (:inverse_rel link))))))

      (conj () entity))))

(defn add-self-link
  [entity-id annotation]
  (let [annotation-id (:_id annotation)]
    (dao/add-self-link (str (dao/entity-single-link entity-id "self") "/annotations/" annotation-id) annotation)))

(defn process-annotations
  [id annotations]

  (map (fn [annotation] (->> annotation
                          (into {})
                          (dao/remove-private-links)
                          (add-self-link id)))
    (annotations/union-annotations-map annotations)))

(defn get-specific-annotations
  "Returns specific annotations associated with entity(id)"
  [api-key id annotation-key]
  (process-annotations id (.get (dao/get-entity-annotations api-key id) annotation-key)))

(defn get-annotations
  "Returns all annotations associated with entity(id)"
  [api-key id]
  (process-annotations id (dao/get-entity-annotations api-key id)))

(defn- update-entity
  [entity dto]
  (let [update (interop/javafy (stringify-keys dto))]
    (.update entity update)
    entity))

(defn update-entity-attributes
  [api-key id attributes]
  (let [entity (get-entity api-key id)
        dto (entity-to-dto entity)
        updated (update-entity entity (assoc-in dto [:attributes] attributes))]
    (into-seq api-key (conj () updated))))

(defn delete-annotation [api-key entity-id annotation-type annotation-id]
  "Deletes an annotation with :annotation-id for entity with id :entity-id"
  (let [entity (get-entity api-key entity-id)
        success (.removeAnnotation entity annotation-type annotation-id)]
    {:success true}))

(defn add-annotation [api-key id annotation-type record]
  "Adds an annotation to an entity"
  (let [entity (get-entity api-key id)]
    (.addAnnotation entity annotation-type record)
    {:success true}))

(defn delete-entity [api-key id]
  (let [entity (-> (ctx api-key) (. getObjectWithUuid (parse-uuid id)))
        trash_resp (-> (ctx api-key) (. trash entity) (.get))]

    {:success (not (empty? trash_resp))}))

(defn get-projects [ctx]
  (.getProjects ctx))

(defn get-sources [ctx]
  (.getTopLevelSources ctx))

(defn get-protocols [ctx]
  (.getProtocols ctx))

(defn index-resource
  [api-key resource]
  (let [resources (case resource
                    "projects" (get-projects (ctx api-key))
                    "sources" (get-sources (ctx api-key))
                    "protocols" (get-protocols (ctx api-key)))]
    (into-seq api-key resources)))

(defn- get-view-results [ctx uri]
  (.getObjectsWithURI ctx uri))

(defn escape-quotes [full-url]
  (clojure.string/replace full-url "\"" "%22"))

(defn get-view
  [api-key full-url]
  (into-seq api-key (get-view-results (ctx api-key) (escape-quotes full-url))))
