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
  [auth rt id rel]
  (map #(relation-summary %) (links/get-link-targets auth id rel rt)))

(defn entity-summary
  "Asynchronously calculates a single-entity summary"
  [auth rt entity]
  (let [id   (:_id entity)
        name (get-in entity [:attributes :name])
        type (:type entity)]
    (case type
      "Activity" (let [inputs  (thread (relations auth rt id k/INPUTS-REL))
                       outputs (thread (relations auth rt id k/OUTPUTS-REL))
                       actions (thread (relations auth rt id k/ACTIONS-REL))]
                   {:_id     id
                    :name    name
                    :type    type
                    :inputs  (<?? inputs)
                    :outputs (<?? outputs)
                    :actions (<?? actions)})
      ;;default
      (let [origins    (thread (relations auth rt id k/ORIGINS-REL))
            activities (thread (relations auth rt id k/ACTIVITIES-REL))]
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
  [auth rt direction entity]
  (if (or (nil? entity) (nil? (:_id entity)))
    []
    (let [desc (entity-summary auth rt entity)
          rels (directional-rels (:type entity) direction)
          next (mapcat #(links/get-link-targets auth (:_id entity) (name %) rt) rels)]

      (concat [desc] (map #(local* auth rt direction %) next)))))

(defn upstream-local
  [auth rt entity]
  (local* auth rt ::upstream entity))

(defn downstream-local
  [auth rt entity]
  (local* auth rt ::downstream entity))

(defn local
  [auth rt ids]
  (let [entities   (core/get-entities auth ids rt)
        upstream   (apply concat (pmap #(upstream-local auth rt %) entities))
        downstream (apply concat (pmap #(downstream-local auth rt %) entities))]

    (concat upstream downstream)))


(defn- project-global
  [auth rt project]
  (let [activities (links/get-link-targets auth project k/ACTIVITIES-REL rt)]
    (pmap #(entity-summary auth rt %) activities)))

(defn global
  [auth rt project-ids]
  (mapcat #(project-global auth rt %) project-ids))
