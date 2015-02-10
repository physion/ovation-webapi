(ns ovation.links
  (:require [ovation.util :refer [create-uri]]
            [ovation.dao :refer [into-seq get-entity]]
            [ring.util.http-response :as r]
            [com.climate.newrelic.trace :refer [defn-traced]]))


(defn get-entities [entity rel]
  (.getEntities entity rel))

(defn-traced get-link
  "Returns all entities from entity(id)->rel and returns them"
  [api-key id rel]
  (into-seq api-key (into () (get-entities (get-entity api-key id) rel))))

(defn-traced add-link
  "Adds a link (:rel) to entity with the given target and inverse"
  [entity rel target & {:keys [inverse] :or {inverse nil}}]

  (.addLink entity rel (create-uri target) inverse)
  true)

(defn-traced remove-link
  "Remoes a link (:rel) from an entity"
  [entity rel target]
  (.removeLink entity rel (create-uri target))
  true)

(defn-traced create-link [api-key id link]
  "Creates a new link from entity(id) -> entity(target)"
  (let [entity (get-entity api-key id)
        target (:target_id link)
        inverse (:inverse_rel link)
        rel (:rel link)]
    (if (add-link entity rel target :inverse inverse)
      {:success true}
      (r/internal-server-error! "Unable to create link"))))

(defn-traced delete-link [api-key id rel target]
  "Deletes a named link on entity(id)"
  (let [entity (get-entity api-key id)]
    (if (remove-link entity rel target)
      {:success true}
      (r/internal-server-error! {:success false}))))


(defn-traced get-named-entities
  "Calls entity.getNamedEntities"
  [entity rel name]
  (.getNamedEntities entity rel name))

(defn-traced add-named-link
  "Adds a named link via entity.addNamedLink"
  [entity rel named target & {:keys [inverse] :or {inverse nil}}]
  (.addNamedLink entity rel named (create-uri target) inverse)
  true)

(defn-traced remove-named-link
  "Removes a named link via entity.removeNamedLink"
  [entity rel named target]
  (.removeNamedLink entity rel named (create-uri target))
  true)

(defn-traced get-named-link
  "Returns all entities from entity(id)->link"
  [api-key id rel named]

  (into-seq api-key (into () (get-named-entities (get-entity api-key id) rel named))))

(defn-traced create-named-link
  "Creates a new link from entity(id) -> entity(target)"
  [api-key id link]
  (let [entity (get-entity api-key id)
        target (:target_id link)
        inverse (:inverse_rel link)
        rel (:rel link)
        named (:name link)]
    (if (add-named-link entity rel named target :inverse inverse)
      [entity]
      (r/internal-server-error! "Unable to create link")))
  )

(defn-traced delete-named-link [api-key id rel named target]
  "Deletes a named link on entity(id)"
  (let [entity (get-entity api-key id)]
    (if (remove-named-link entity rel named target)
      {:success true}
      (r/internal-server-error! {:success false}))))
