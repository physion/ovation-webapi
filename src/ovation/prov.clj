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
  [ctx db id rel]
  (map #(relation-summary %) (links/get-link-targets ctx db id rel)))

(defn entity-summary
  "Asynchronously calculates a single-entity summary"
  [ctx db entity]
  (let [id   (:_id entity)
        name (get-in entity [:attributes :name])
        type (:type entity)]
    (case type
      "Activity" (let [inputs  (thread (relations ctx db id k/INPUTS-REL))
                       outputs (thread (relations ctx db id k/OUTPUTS-REL))
                       actions (thread (relations ctx db id k/ACTIONS-REL))]
                   {:_id     id
                    :name    name
                    :type    type
                    :inputs  (<?? inputs)
                    :outputs (<?? outputs)
                    :actions (<?? actions)})
      ;;default
      (let [origins    (thread (relations ctx db id k/ORIGINS-REL))
            activities (thread (relations ctx db id k/ACTIVITIES-REL))]
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
  [ctx db direction entity]
  (if (or (nil? entity) (nil? (:_id entity)))
    []
    (let [desc (entity-summary ctx db entity)
          rels (directional-rels (:type entity) direction)
          next (apply concat (pmap #(links/get-link-targets ctx db (:_id entity) (name %)) rels))]

      (concat [desc] (mapcat #(local* ctx db direction %) next)))))

(defn upstream-local
  [ctx db entity]
  (local* ctx db ::upstream entity))

(defn downstream-local
  [ctx db entity]
  (local* ctx db ::downstream entity))

(defn local
  [ctx db ids]
  (let [entities    (core/get-entities ctx db ids)
        upstream    (apply concat (pmap #(upstream-local ctx db %) entities))
        downstream  (apply concat (pmap #(downstream-local ctx db %) entities))
        results     (concat upstream downstream)
        results-map (into {} (map (fn [s] [(:_id s) s]) results))]

    (vals results-map)))


(defn- project-global
  [ctx db project]
  (let [activities (links/get-link-targets ctx db project k/ACTIVITIES-REL)]
    (pmap #(entity-summary ctx db %) activities)))

(defn global
  [ctx db project-ids]
  (mapcat #(project-global ctx db %) project-ids))
