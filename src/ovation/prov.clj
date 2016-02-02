(ns ovation.prov
  (:require [ovation.links :as links]
            [ovation.constants :as k]
            [ovation.core :as core]))

(defn- activity-relation-summary
  [f]
  {:_id  (:_id f)
   :type (:type f)
   :name (get-in f [:attributes :name])})


(defn- relations
  [auth rt id rel]
  (map #(activity-relation-summary %) (links/get-link-targets auth id rel rt)))

(defn- activity-summary
  [auth rt activity]
  (let [id (:_id activity)
        inputs (future (relations auth rt id k/INPUTS-REL))
        outputs (future (relations auth rt id k/OUTPUTS-REL))
        actions (future (relations auth rt id k/ACTIONS-REL))]
    {:_id     id
     :name    (get-in activity [:attributes :name])
     :type    (:type activity)
     :inputs  @inputs
     :outputs @outputs
     :actions @actions}))



;(defn local
;  [auth rt ids]
;  [])


(defn- project-global
  [auth rt project]
  (let [activities (links/get-link-targets auth project k/ACTIVITIES-REL rt)]
    (pmap #(activity-summary auth rt %) activities)))

(defn global
  [auth rt project-ids]
  (mapcat #(project-global auth rt %) project-ids))
