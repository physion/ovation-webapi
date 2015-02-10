(ns ovation.entity
  (:import (us.physion.ovation.domain URIs)
           (us.physion.ovation.domain OvationEntity$AnnotationKeys)
           (us.physion.ovation.exceptions OvationException)
           (us.physion.ovation.util MultimapUtils))
  (:require [clojure.walk :refer [stringify-keys]]
            [ovation.dao :refer [get-entity entity-to-dto into-seq]]
            [ovation.util :refer [ctx create-uri parse-uuid]]
            [slingshot.slingshot :refer [try+ throw+]]
            [ovation.context :refer [transaction]]
            [ovation.links :as links]
            [ovation.interop :as interop]
            [ovation.annotations :as annotations]
            [ovation.dao :as dao]
            [com.climate.newrelic.trace :refer [defn-traced]]))


(defn create-multimap [m]
  (MultimapUtils/createMultimap m))

(defn-traced insert-entity
  "Inserts dto as an entity into the given DataContext"
  [context dto]
  (-> context (.insertEntity dto)))

(defn-traced create-entity
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

          (into-seq api-key (conj () entity)))))))

(defn-traced add-self-link
  [entity-id annotation]
  (let [annotation-id (:_id annotation)]
    (dao/add-self-link (str (dao/entity-single-link entity-id "self") "/annotations/" annotation-id) annotation)))

(defn-traced process-annotations
  [id annotations]

  (map (fn [annotation] (->> annotation
                          (into {})
                          (dao/remove-private-links)
                          (add-self-link id)))
    (annotations/union-annotations-map annotations)))

(defn-traced get-specific-annotations
  "Returns specific annotations associated with entity(id)"
  [api-key id annotation-key]
  (process-annotations id (.get (dao/get-entity-annotations api-key id) annotation-key)))

(defn-traced get-annotations
  "Returns all annotations associated with entity(id)"
  [api-key id]
  (process-annotations id (dao/get-entity-annotations api-key id)))

(defn- update-entity
  [entity dto]
  (let [update (interop/javafy (stringify-keys dto))]
    (.update entity update)
    entity))

(defn-traced update-entity-attributes
  [api-key id attributes]
  (let [entity (get-entity api-key id)
        dto (entity-to-dto entity)
        updated (update-entity entity (assoc-in dto [:attributes] attributes))]
    (into-seq api-key (conj () updated))))

(defn-traced delete-annotation [api-key entity-id annotation-type annotation-id]
  "Deletes an annotation with :annotation-id for entity with id :entity-id"
  (let [entity (get-entity api-key entity-id)
        success (.removeAnnotation entity annotation-type annotation-id)]
    {:success true}))

(defn-traced add-annotation [api-key id annotation-type record]
  "Adds an annotation to an entity"
  (let [entity (get-entity api-key id)]
    (.addAnnotation entity annotation-type record)
    {:success true}))

(defn-traced delete-entity [api-key id]
  (let [entity (-> (ctx api-key) (. getObjectWithUuid (parse-uuid id)))
        trash_resp (-> (ctx api-key) (. trash entity) (.get))]

    {:success (not (empty? trash_resp))}))

(defn-traced get-projects [ctx]
  (.getProjects ctx))

(defn-traced get-sources [ctx]
  (.getTopLevelSources ctx))

(defn-traced get-protocols [ctx]
  (.getProtocols ctx))

(defn-traced index-resource
  [api-key resource]
  (let [resources (case resource
                    "projects" (get-projects (ctx api-key))
                    "sources" (get-sources (ctx api-key))
                    "protocols" (get-protocols (ctx api-key)))]
    (into-seq api-key resources)))

(defn- get-view-results [ctx uri]
  (.getObjectsWithURI ctx uri))

(defn-traced escape-quotes [full-url]
  (clojure.string/replace full-url "\"" "%22"))

(defn-traced get-view
  [api-key full-url]
  (into-seq api-key (get-view-results (ctx api-key) (escape-quotes full-url))))
