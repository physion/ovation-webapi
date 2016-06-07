(ns ovation.search
  (:require [ovation.couch :as couch]
            [ovation.constants :as k]
            [ovation.core :as core]))

(defn get-results
  [auth routes rows]
  (let [ids (map (fn [r]
                     (condp = (get-in r [:fields :type])
                       k/ANNOTATION-TYPE (get-in r [:fields :entity])
                       ;; default
                       (:id r)) rows))]
    (core/get-entities auth ids routes)))

(defn search
  [auth rt q]
  (let [db       (couch/db auth)
        raw      (couch/search db q)
        entities (get-results auth rt (:rows raw))]
    {:metadata {:total_rows (:total_rows raw)
                :bookmark   (:bookmark raw)}
     :data     entities}))

