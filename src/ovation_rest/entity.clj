(ns ovation-rest.entity
  (:import (us.physion.ovation.domain URIs)
           (us.physion.ovation.exceptions OvationException))
  (:require [clojure.walk :refer [stringify-keys]]
            [ovation-rest.util :refer :all]
            [slingshot.slingshot :refer [try+ throw+]]
            [ovation-rest.context :refer [transaction]]
            [ovation-rest.links :as links]))


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
          (doseq [[rel rel-links] links]
            (doseq [link rel-links]
              (links/add-link entity (name rel) (create-uri (:target_id link)) :inverse (:inverse_rel link))))

          ;; For all named links, add the named link
          (doseq [[rel names] named-links]
            (doseq [[named rel-links] names]
              (doseq [link rel-links]
                (links/add-named-link entity (name rel) (name named) (create-uri (:target_id link)) :inverse (:inverse_rel link)))))

          (into-seq (conj () entity)))))))

(defn get-annotations [api-key id]
  "Returns all annotations associated with entity(id)"
  (into [] (.getAnnotations (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id))))))

(defn update-entity [api-key id dto]
  (let [entity (-> (ctx api-key) (.getObjectWithUuid (parse-uuid id)))]
    (.update entity (stringify-keys (update-in dto [:links] create-multimap)))
    (into-seq [entity])
    ))

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
  (.getObjectsWithURI ctx (clojure.string/replace uri "\"" "%22")))

(defn get-view [api-key full-url host-url]
  (into-seq (get-view-results (ctx api-key) full-url)))

