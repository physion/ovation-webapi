;(ns ovation.entity
;  (:require [clojure.walk :refer [stringify-keys]]
;            [ovation.dao :refer [get-entity entity-to-dto into-seq]]
;            [ovation.util :refer [ctx create-uri parse-uuid]]
;            [slingshot.slingshot :refer [try+ throw+]]
;            [ovation.context :refer [transaction]]
;            [ovation.links :as links]
;            [ovation.interop :as interop]
;            [ovation.annotations :as annotations]
;            [ovation.version :as ver]
;            [ovation.dao :as dao]
;            [com.ashafa.clutch :as cl]
;            [ovation.couch :as couch]
;            [clojure.core.async :as async :refer [chan promise-chan <!! >!! timeout alt!!]]))
;
;
;
;(defn filter-trashed
;  "Removes entity documents with a non-nil trash_info from seq"
;  [entities include_trashed]
;  (filter #(or include_trashed (nil? (:trash_info %))) entities))
;
;(defn of-type
;  "Gets all entities of the given type"
;  [auth resource & {:keys [include_trashed] :or {include_trashed false}}]
;
;  (-> (map :doc (cl/with-db (couch/db auth)
;                  (cl/get-view couch/design-doc us.physion.ovation.data.EntityDao$Views/ENTITIES_BY_TYPE {:key resource :reduce false :include_docs true})))
;    (couch/transform)
;    (filter-trashed include_trashed)))
;
;
;(defn get-entities
;  "Gets entities by ID"
;  [auth entity-ids & {:keys [include_trashed] :or {include_trashed false}}]
;
;  (-> (map :doc (cl/with-db (couch/db auth)
;                  (cl/all-documents {:include_docs true} {:keys entity-ids})))
;    (couch/transform)
;    (filter-trashed include_trashed)))
;
;
;(defn make-uuid
;  "Wraps java.util.UUID/randomUUID for test mocking."
;  []
;  (java.util.UUID/randomUUID))
;
;(defn -ensure-id
;  "Makes sure there's an _id for entity"
;  [doc]
;  (if (nil? (:_id doc))
;    (assoc doc :_id (make-uuid))
;    doc))
;
;(defn -ensure-api-version
;  "Insert API version"
;  [doc]
;  (assoc doc :api_version ver/schema-version))
;
;(defn -add-owner
;  "Adds owner link document."
;
;  [auth doc]
;
;  ;;TODO white owner link document to channel
;  doc)
;
;(defn add-collaboration-roots
;  [doc roots]
;  (assoc-in doc [:links :_collaboration_roots] roots))
;
;(defn collaboration-roots
;  [auth doc]
;
;  ;;TODO
;  nil)
;
;(defn insert-entity                                         ;; TODO make this insert-entities
;  "Inserts dto as an entity into the given DataContext"
;  [auth raw_dto]
;
;  ;; 1. Collect in-memory links
;  ;; 2. Start collaboration roots, one per entity (including in-memory links)
;  ;; 3. transform pipeline
;  ;; TODO
;  ;; ensure owner
;  ;; link documents from dto
;  ;; collaboration_roots (**added to all dtos, including links**)
;
;  (let [links (:links raw_dto)
;        named_links (:named_links raw_dto)
;        dto (->> (dissoc raw_dto :links :named_links)
;              (-ensure-id)
;              (-ensure-api-version)
;              (-add-collaboration-roots auth))
;        owner-link (links/make-link-doc doc (:user_id auth) :owner) ;;TODO :user_id?, inverse?
;        collab-roots (collaboration-roots auth dto)]
;
;    (cl/with-db (couch/db auth)
;      (cl/bulk-update (map #(add-collaboration-roots % collab-roots) [dto owner-link])))))
;
;(defn create-entity
;  "Creates a new Entity from a DTO map"
;  [auth api-key new-dto]
;
;  (let [links (:links new-dto)
;        named-links (:named_links new-dto)
;        dto (stringify-keys (dissoc new-dto :links :named_links))]
;
;    (let [entity (insert-entity auth dto)]
;      ;; For all links, add the link
;      (when links
;        (doseq [[rel rel-links] links]
;          (doseq [link rel-links]
;            (links/add-link entity (name rel) (create-uri (:target_id link)) :inverse (:inverse_rel link)))))
;
;      ;; For all named links, add the named link
;      (when named-links
;        (doseq [[rel names] named-links]
;          (doseq [[named rel-links] names]
;            (doseq [link rel-links]
;              (links/add-named-link entity (name rel) (name named) (create-uri (:target_id link)) :inverse (:inverse_rel link))))))
;
;      (conj () entity))))
;
;(defn add-self-link
;  [entity-id annotation]
;  (let [annotation-id (:_id annotation)]
;    (dao/add-self-link (str (dao/entity-single-link entity-id "self") "/annotations/" annotation-id) annotation)))
;
;(defn process-annotations
;  [id annotations]
;
;  (map (fn [annotation] (->> annotation
;                          (into {})
;                          (dao/remove-private-links)
;                          (add-self-link id)))
;    (annotations/union-annotations-map annotations)))
;
;(defn get-specific-annotations
;  "Returns specific annotations associated with entity(id)"
;  [api-key id annotation-key]
;  (process-annotations id (.get (dao/get-entity-annotations api-key id) annotation-key)))
;
;(defn get-annotations
;  "Returns all annotations associated with entity(id)"
;  [api-key id]
;  (process-annotations id (dao/get-entity-annotations api-key id)))
;
;(defn- update-entity
;  [entity dto]
;  (let [update (interop/javafy (stringify-keys dto))]
;    (.update entity update)
;    entity))
;
;(defn update-entity-attributes
;  [api-key id attributes]
;  (let [entity (get-entity api-key id)
;        dto (entity-to-dto entity)
;        updated (update-entity entity (assoc-in dto [:attributes] attributes))]
;    (into-seq api-key (conj () updated))))
;
;(defn delete-annotation [api-key entity-id annotation-type annotation-id]
;  "Deletes an annotation with :annotation-id for entity with id :entity-id"
;  (let [entity (get-entity api-key entity-id)
;        success (.removeAnnotation entity annotation-type annotation-id)]
;    {:success true}))
;
;(defn add-annotation [api-key id annotation-type record]
;  "Adds an annotation to an entity"
;  (let [entity (get-entity api-key id)]
;    (.addAnnotation entity annotation-type record)
;    {:success true}))
;
;(defn delete-entity [api-key id]
;  (let [entity (-> (ctx api-key) (. getObjectWithUuid (parse-uuid id)))
;        trash_resp (-> (ctx api-key) (. trash entity) (.get))]
;
;    {:success (not (empty? trash_resp))}))
