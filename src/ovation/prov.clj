(ns ovation.prov
  (:require [ovation.links :as links]
            [ovation.constants :as k]))

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
  (let [id (:_id activity)]
    {:_id     id
     :name    (get-in activity [:attributes :name])
     :inputs  (relations auth rt id k/INPUTS-REL)
     :outputs (relations auth rt id k/OUTPUTS-REL)
     :actions (relations auth rt id k/ACTIONS-REL)}))

(defn- project-global
  [auth rt project]
  (let [activities (links/get-link-targets auth project k/ACTIVITIES-REL rt)]
    (map #(activity-summary auth rt %) activities)))

(defn global
  [auth rt projects]
  (mapcat #(project-global auth rt %) projects))
