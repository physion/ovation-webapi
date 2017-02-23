(ns ovation.prov
  (:require [ovation.links :as links]
            [ovation.constants :as k]
            [ovation.core :as core]
            [clojure.core.async :as async :refer [chan go thread]]
            [ovation.util :refer [<??]]))

(defn- relation-summary
  [f]
  {:_id  (:_id f)
   :type (:type f)
   :name (get-in f [:attributes :name])})


(defn- relations
  [auth db rt id rel]
  (map #(relation-summary %) (links/get-link-targets auth db id rel rt)))

(defn entity-summary
  "Asynchronously calculates a single-entity summary"
  [auth db rt entity]
  (let [id   (:_id entity)
        name (get-in entity [:attributes :name])
        type (:type entity)]
    (case type
      "Activity" (let [inputs  (thread (relations auth db rt id k/INPUTS-REL))
                       outputs (thread (relations auth db rt id k/OUTPUTS-REL))
                       actions (thread (relations auth db rt id k/ACTIONS-REL))]
                   {:_id     id
                    :name    name
                    :type    type
                    :inputs  (<?? inputs)
                    :outputs (<?? outputs)
                    :actions (<?? actions)})
      ;;default
      (let [origins    (thread (relations auth db rt id k/ORIGINS-REL))
            activities (thread (relations auth db rt id k/ACTIVITIES-REL))]
        {:_id        id
         :name       name
         :type       type
         :origins    (<?? origins)
         :activities (<?? activities)}))))


(defn directional-rels
  [type dir]
  (dir (case type
         "Activity" {::upstream   #{:inputs}
                     ::downstream #{:outputs}}
         ;;default
         {::upstream   #{:origins}
          ::downstream #{:activities}})))

(defn local*
  [auth db rt direction entity]
  (if (or (nil? entity) (nil? (:_id entity)))
    []
    (let [desc (entity-summary auth db rt entity)
          rels (directional-rels (:type entity) direction)
          next (apply concat (pmap #(links/get-link-targets auth db (:_id entity) (name %) rt) rels))]

      (concat [desc] (mapcat #(local* auth db rt direction %) next)))))

(defn upstream-local
  [auth db rt entity]
  (local* auth db rt ::upstream entity))

(defn downstream-local
  [auth db rt entity]
  (local* auth db rt ::downstream entity))

(defn local
  [auth db rt ids]
  (let [entities    (core/get-entities auth db ids rt)
        upstream    (apply concat (pmap #(upstream-local auth db rt %) entities))
        downstream  (apply concat (pmap #(downstream-local auth db rt %) entities))
        results     (concat upstream downstream)
        results-map (into {} (map (fn [s] [(:_id s) s]) results))]

    (vals results-map)))


(defn- project-global
  [auth db rt project]
  (let [activities (links/get-link-targets auth db project k/ACTIVITIES-REL rt)]
    (pmap #(entity-summary auth db rt %) activities)))

(defn global
  [auth  db rt project-ids]
  (mapcat #(project-global auth db rt %) project-ids))
