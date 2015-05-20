(ns ovation.links
  (:require [ovation.util :refer [create-uri]]))




(defn get-link-targets
  [db id rel & {:keys [label] :or {label nil}}]

  )

(defn add-link
  [db id rel target-id])

(defn delete-link
  [db id rel target-id])

;(defn get-link
;  "Returns all entities from entity(id)->rel and returns them"
;  [api-key id rel]
;  (into-seq api-key (into () (get-entities (get-entity api-key id) rel))))
;
;(defn add-link
;  "Adds a link (:rel) to entity with the given target and inverse"
;  [entity rel target & {:keys [inverse] :or {inverse nil}}]
;
;  (.addLink entity rel (create-uri target) inverse)
;  true)
;
;(defn remove-link
;  "Remoes a link (:rel) from an entity"
;  [entity rel target]
;  (.removeLink entity rel (create-uri target))
;  true)
;
;(defn create-link [api-key id link]
;  "Creates a new link from entity(id) -> entity(target)"
;  (let [entity (get-entity api-key id)
;        target (:target_id link)
;        inverse (:inverse_rel link)
;        rel (:rel link)]
;    (if (add-link entity rel target :inverse inverse)
;      {:success true}
;      (r/internal-server-error! "Unable to create link"))))
;
;(defn delete-link [api-key id rel target]
;  "Deletes a named link on entity(id)"
;  (let [entity (get-entity api-key id)]
;    (if (remove-link entity rel target)
;      {:success true}
;      (r/internal-server-error! {:success false}))))
;
;
;(defn get-named-entities
;  "Calls entity.getNamedEntities"
;  [entity rel name]
;  (.getNamedEntities entity rel name))
;
;(defn add-named-link
;  "Adds a named link via entity.addNamedLink"
;  [entity rel named target & {:keys [inverse] :or {inverse nil}}]
;  (.addNamedLink entity rel named (create-uri target) inverse)
;  true)
;
;(defn remove-named-link
;  "Removes a named link via entity.removeNamedLink"
;  [entity rel named target]
;  (.removeNamedLink entity rel named (create-uri target))
;  true)
;
;(defn get-named-link
;  "Returns all entities from entity(id)->link"
;  [api-key id rel named]
;
;  (into-seq api-key (into () (get-named-entities (get-entity api-key id) rel named))))
;
;(defn create-named-link
;  "Creates a new link from entity(id) -> entity(target)"
;  [api-key id link]
;  (let [entity (get-entity api-key id)
;        target (:target_id link)
;        inverse (:inverse_rel link)
;        rel (:rel link)
;        named (:name link)]
;    (if (add-named-link entity rel named target :inverse inverse)
;      [entity]
;      (r/internal-server-error! "Unable to create link")))
;  )
;
;(defn delete-named-link [api-key id rel named target]
;  "Deletes a named link on entity(id)"
;  (let [entity (get-entity api-key id)]
;    (if (remove-named-link entity rel named target)
;      {:success true}
;      (r/internal-server-error! {:success false}))))
