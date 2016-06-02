(ns ovation.breadcrumbs
  (:require [ovation.links :as links]
            [ovation.constants :as k]
            [ubergraph.core :as uber]
            [ubergraph.alg :as alg]
            [clojure.pprint :refer [pprint]]
            [ovation.core :as core]
            [ovation.util :as util]))


(defn build-graph
  "Builds a directed graph of child -> parent nodes using loop/recur. Returns the Ubergraph."
  [auth routes entity-ids graph]
  (loop [ids entity-ids
         g   graph]
    (let [edges        (pmap (fn [id]
                               (let [parents (links/get-link-targets auth id k/PARENTS-REL routes)]
                                 (map (fn [parent] [id (:_id parent)]) parents))) ids)
          parent-edges (apply concat edges)
          parent-ids   (set (map #(second %) parent-edges))]

      (if (empty? ids)
        g
        (recur parent-ids (uber/add-edges* g parent-edges))))))

(defn collect-paths
  "Finds all paths from ids to parents"
  [auth graph ids routes]
  (let [entities (util/into-id-map (core/get-entities auth (uber/nodes graph) routes))
        spans (apply conj (map (fn [start]
                                 (let [span (alg/pre-span graph start)
                                       _    (pprint span)]
                                   {start span}))
                             ids))]))



(defn get-parents
  [auth routes doc]
  (links/get-link-targets auth (:_id doc) k/PARENTS-REL routes))

(defn get-breadcrumbs
  "Gets all breadcrumb paths to entities with IDs `ids`"
  [auth routes ids]
  (let [graph (build-graph auth routes ids (uber/digraph))]
    (collect-paths auth graph ids routes)))




