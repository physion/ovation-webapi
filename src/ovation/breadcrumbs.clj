(ns ovation.breadcrumbs
  (:require [ovation.links :as links]
            [ovation.constants :as k]
            [ubergraph.core :as uber]
            [clojure.pprint :refer [pprint]]
            [ovation.core :as core]
            [ovation.util :as util]
            [com.climate.newrelic.trace :refer [defn-traced]]))


(defn-traced get-parents
  [ctx db id]
  (let [parents (links/get-link-targets ctx db id k/PARENTS-REL)]
    parents))


(defn-traced build-graph
  "Builds a directed graph of child -> parent nodes using loop/recur. Returns the Ubergraph."
  [ctx db entity-ids g]
  (let [graph (apply uber/add-nodes g entity-ids)]
    (loop [ids entity-ids
           g   graph]
      (let [edges        (pmap (fn [id]
                                 (let [parents (get-parents ctx db id)]
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
     :organization (:organization e)
     :name (get-in e [:attributes :name])}))

(defn- extend-path
  [id graph entities path]
  (let [head       (-> path last :id)
        next-nodes (map :dest (uber/out-edges graph head))]
    (if (empty? next-nodes)
      [path]
      (mapcat (fn [n] (extend-path id graph entities (conj path (make-node-description n entities)))) next-nodes))))


(defn-traced collect-paths
  "Finds all paths from ids to parents"
  [ctx db graph ids]
  (let [entities (util/into-id-map (core/get-entities ctx db (uber/nodes graph)))]
    (into {} (map (fn [id]
                    (let [paths (extend-path id graph entities [(make-node-description id entities)])]
                      [id paths]))
               ids))))


(defn-traced get-breadcrumbs
  "Gets all breadcrumb paths to entities with IDs `ids`"
  [ctx db ids]
  (let [graph  (build-graph ctx db ids (uber/digraph))
        result (collect-paths ctx db graph ids)]
    result))



