(ns ovation-rest.links
  (:import (us.physion.ovation.domain URIs))
  (:require [ovation-rest.util :refer :all]
            [ovation-rest.entity :refer [get-entity]]
            [ring.util.http-response :as r]))


(defn get-entities [entity rel]
  (.getEntities entity rel))

(defn get-link [api-key id rel]
  "Returns all entities from entity(id)->rel and returns them"
  (into-seq (into () (get-entities (get-entity api-key id) rel))))

(defn add-link
  "Adds a link (:rel) to entity with the given target and inverse"
  [entity rel target & {:keys [inverse] :or {inverse nil}}]

  (.addLink entity rel (create-uri target) inverse)
  true)

(defn remove-link
  "Remoes a link (:rel) from an entity"
  [entity rel target]
  (.removeLink entity rel (create-uri target))
  true)

(defn create-link [api-key id rel link]
  "Creates a new link from entity(id) -> entity(target)"
  (let [entity (get-entity api-key id)
        target (:target_id link)
        inverse (:inverse_rel link)]
    (if (add-link entity rel target :inverse inverse)
      [entity]
      (r/internal-server-error! "Unable to create link"))))

(defn delete-link [api-key id rel target]
  "Deletes a named link on entity(id)"
  (let [entity (get-entity api-key id)]
    (if (remove-link entity rel target)
      {:success true}
      (r/internal-server-error! {:success false}))))

(defn get-named-link [api-key id rel named inverse]
  "Returns all entities from entity(id)->link"
  (into-seq (into [] (.getNamedEntities (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id))) rel named))))

(defn create-named-link [api-key id rel target inverse]
  "Creates a new link from entity(id) -> entity(target)"
  (let [entity ((ctx api-key) (.getObjectWithUuid (parse-uuid id)))
        linked (.addNamedLink entity rel (create-uri target) inverse)]
    (into-seq (seq [linked]))))

(defn delete-named-link [api-key id rel named target]
  "Deletes a named link on entity(id)"
  (let [entity (-> (ctx api-key) (. getObjectWithUuid (parse-uuid id)))
        delete (.removeNamedLink entity rel named (create-uri target))]
    {:success (not (empty? delete))}))
