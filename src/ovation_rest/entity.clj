(ns ovation-rest.entity
  (:import (us.physion.ovation.domain URIs)
           (us.physion.ovation.domain OvationEntity$AnnotationKeys)
           (us.physion.ovation.exceptions OvationException))
  (:require [clojure.walk :refer [stringify-keys]]
            [ovation-rest.util :refer [ctx get-entity entity-to-dto create-uri parse-uuid into-seq]]
            [slingshot.slingshot :refer [try+ throw+]]
            [ovation-rest.context :refer [transaction]]
            [ovation-rest.links :as links]
            [ovation-rest.interop :as interop]))


(defn create-multimap [m]
  (us.physion.ovation.util.MultimapUtils/createMultimap m))

(defn insert-entity
  "Inserts dto as an entity into the given DataContext"
  [context dto]
  (-> context (.insertEntity dto)))

(defn create-entity
  "Creates a new Entity from a DTO map"
  [api-key new-dto]

  (let [links (:links new-dto)
        named-links (:named_links new-dto)
        dto (stringify-keys (dissoc new-dto :links :named_links))]
    (let [c (ctx api-key)]
      (transaction c
        (let [entity (insert-entity c dto)]
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

          (into-seq (conj () entity)))))))

(defn get-annotations [api-key id]
  "Returns all annotations associated with entity(id)"
  (into [] (.getAnnotations (get-entity api-key id))))

(defn- update-entity
  [entity dto]
  (let [update (interop/javafy (stringify-keys dto))]
    (.update entity update)
    entity))

(defn update-entity-attributes [api-key id attributes]
  (let [entity (get-entity api-key id)
        dto (entity-to-dto entity)
        updated (update-entity entity (assoc-in dto [:attributes] attributes))]
    (into-seq (conj () updated))))

(defn delete-annotation [api-key entity-id annotation-type annotation-id]
  "Deletes an annotation with :annotation-id for entity with id :entity-id"
  (let [entity  (get-entity api-key entity-id)
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

(defn- ^{:testable true} get-projects [ctx]
  (.getProjects ctx))

(defn- ^{:testable true} get-sources [ctx]
  (.getTopLevelSources ctx))

(defn- ^{:testable true} get-protocols [ctx]
  (.getProtocols ctx))

(defn index-resource [api-key resource]
  (let [resources (case resource
                    "projects" (get-projects (ctx api-key))
                    "sources" (get-sources (ctx api-key))
                    "protocols" (get-protocols (ctx api-key)))]
    (into-seq resources)))

(defn- get-view-results [ctx uri]
  (.getObjectsWithURI ctx uri))

(defn escape-quotes [full-url]
  (clojure.string/replace full-url "\"" "%22"))

(defn get-view [api-key full-url]
  (into-seq (get-view-results (ctx api-key) (escape-quotes full-url))))

