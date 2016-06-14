(ns ovation.search
  (:require [ovation.couch :as couch]
            [ovation.constants :as k]
            [ovation.core :as core]
            [ovation.breadcrumbs :as breadcrumbs]
            [ovation.routes :as routes]
            [ovation.links :as links]
            [ovation.util :as util]))

(defn entity-ids
  [rows]
  (map (fn [r]
         (condp = (get-in r [:fields :type])
           k/ANNOTATION-TYPE (get-in r [:fields :id])
           ;; default
           (:id r))) rows))

(defn breadcrumbs-url
  [routes id]
  (str (routes/named-route routes :get-breadcrumbs {}) "?id=" id))

(defn get-results
  [auth routes rows]
  (let [ids (entity-ids rows)
        entities (core/get-entities auth ids routes)
        root-ids (mapcat #(links/collaboration-roots %) entities)
        roots (util/into-id-map (core/get-entities auth root-ids routes))]
    (map (fn [entity] {:id            (:_id entity)
                       :entity_type   (:type entity)
                       :name          (get-in entity [:attributes :name] (:_id entity))
                       :owner         (:owner entity)
                       :updated-at    (get-in entity [:attributes :updated-at])
                       :project_names (if-let [collaboration-roots (links/collaboration-roots entity)]
                                        (remove nil? (map (fn [root-id] (get-in (get roots root-id) [:attributes :name])) collaboration-roots))
                                        [])
                       :links         {:breadcrumbs (breadcrumbs-url routes (:_id entity))}}) entities)))

(defn search
  [auth rt q & {:keys [bookmark] :or {bookmark nil}}]
  (let [db       (couch/db auth)
        raw      (couch/search db q :bookmark bookmark)
        entities (get-results auth rt (:rows raw))]
    {:meta           {:total_rows (:total_rows raw)
                      :bookmark   (:bookmark raw)}
     :search_results entities}))

