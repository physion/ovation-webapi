(ns ovation.breadcrumbs
  (:require [ovation.links :as links]
            [ovation.constants :as k]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [clojure.pprint :refer [pprint]]
            [ovation.core :as core]
            [ovation.util :as util]))


(defn get-parents
  [auth id routes]
  (let [parents (links/get-link-targets auth id k/PARENTS-REL routes)]
    parents))


(defn build-graph
  "Builds a directed graph of child -> parent nodes using loop/recur. Returns the Ubergraph."
  [auth routes entity-ids g]
  (let [graph (apply uber/add-nodes g entity-ids)]
    (loop [ids entity-ids
           g   graph]
      (let [edges        (pmap (fn [id]
                                 (let [parents (get-parents auth id routes)]
                                   (map (fn [parent] [id (:_id parent)]) parents))) ids)
            parent-edges (apply concat edges)
            parent-ids   (set (map #(second %) parent-edges))]

        (if (empty? ids)
          g
          (recur parent-ids (uber/add-edges* g parent-edges)))))))

(defn make-node-description
  [id entities]
  (let [e (get entities id {})]
    {:type (:type e)
     :id id
     :name (get-in e [:attributes :name])}))

(defn- extend-path
  [id graph entities path]
  (let [head       (-> path last :id)
        next-nodes (map :dest (uber/out-edges graph head))]
    (if (empty? next-nodes)
      [path]
      (mapcat (fn [n] (extend-path id graph entities (conj path (make-node-description n entities)))) next-nodes))))


(defn collect-paths
  "Finds all paths from ids to parents"
  [auth graph ids routes]
  (let [entities (util/into-id-map (core/get-entities auth (uber/nodes graph) routes))]
    (into {} (map (fn [id]
                    (let [paths (extend-path id graph entities [(make-node-description id entities)])]
                      [id paths]))
               ids))))


(defn get-breadcrumbs
  "Gets all breadcrumb paths to entities with IDs `ids`"
  [auth routes ids]
  (let [graph  (build-graph auth routes ids (uber/digraph))
        result (collect-paths auth graph ids routes)]
    result))


