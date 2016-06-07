(ns ovation.search
  (:require [ovation.couch :as couch]
            [ovation.constants :as k]
            [ovation.core :as core]))

(defn entity-ids
  [rows]
  (map (fn [r]
         (condp = (get-in r [:fields :type])
           k/ANNOTATION-TYPE (get-in r [:fields :entity])
           ;; default
           (:id r))) rows))

(defn get-results
  [auth routes rows]
  (let [ids (entity-ids rows)
        breadcrumbs (ovation.breadcrumbs/get-breadcrumbs auth routes ids)]
    (map (fn [entity] {:id          (:_id entity)
                       :type        (:type entity)
                       :breadcrumbs (get breadcrumbs (:_id entity))}) (core/get-entities auth ids routes))))

(defn search
  [auth rt q & {:keys [bookmark] :or {bookmark nil}}]
  (let [db       (couch/db auth)
        raw      (couch/search db q)
        entities (get-results auth rt (:rows raw))]
    {:metadata {:total_rows (:total_rows raw)
                :bookmark   (:bookmark raw)}
     :data     entities}))

