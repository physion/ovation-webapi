(ns ovation.breadcrumbs
  (:require [ovation.links :as links]
            [ovation.constants :as k]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [clojure.pprint :refer [pprint]]
            [ovation.core :as core]
            [ovation.util :as util]))


(defn get-parents
  [auth routes id]
  (links/get-link-targets auth id k/PARENTS-REL routes))


(defn build-graph
  "Builds a directed graph of child -> parent nodes using loop/recur. Returns the Ubergraph."
  [auth routes entity-ids graph]
  (loop [ids entity-ids
         g   graph]
    (let [edges        (pmap (fn [id]
                               (let [parents (get-parents auth id routes)]
                                 (map (fn [parent] [id (:_id parent)]) parents))) ids)
          parent-edges (apply concat edges)
          parent-ids   (set (map #(second %) parent-edges))]

      (if (empty? ids)
        g
        (recur parent-ids (uber/add-edges* g parent-edges))))))

(defn make-node-description
  [id entities]
  (let [e (get entities id {})]
    {:type (:type e)
     :id id
     :name (get-in e [:attributes :name])}))

(defn- extend-path
  [id spans entities path]
  (let [head (-> path last :id)
        next-nodes (get-in spans [id head])]
    (if (nil? next-nodes)
      path
      (let [result (map (fn [n]
                          (conj path (make-node-description n entities))) next-nodes)]
        result))))

(defn- extend-paths
  [id spans entities starting-paths]
  (loop [paths starting-paths]
    (let [
          _ (println paths)
          head-nodes (map (fn [n] (-> n last :id)) paths)
          next-nodes (doall (map #(get-in spans [id %]) head-nodes))]
         (if (every? nil? next-nodes)
           paths
           (recur (map #(extend-path id spans entities %) paths))))))



(defn collect-paths
  "Finds all paths from ids to parents"
  [auth graph ids routes]
  (let [entities (util/into-id-map (core/get-entities auth (uber/nodes graph) routes))
        spans (apply conj (map (fn [start]
                                 (let [span (alg/pre-span graph start)]
                                   {start span}))
                             ids))]
    (into {} (map (fn [id] [id (extend-paths id spans entities [[(make-node-description id entities)]])]) ids))))


(defn get-breadcrumbs
  "Gets all breadcrumb paths to entities with IDs `ids`"
  [auth routes ids]
  (let [graph (build-graph auth routes ids (uber/digraph))]
    (collect-paths auth graph ids routes)))




