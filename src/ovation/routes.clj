(ns ovation.routes
  (:require [compojure.api.routes :refer [path-for*]]
            [ovation.util :as util]))

(defn router
  [request]
  (fn [name & [params]]
    (path-for* name request params)))

(defn relationship-route
  [rt doc name]
  (let [type (util/entity-type-name doc)
        id (:_id doc)]
    (rt (keyword (format "get-%s-links" type)) {:id id :rel name})))

(defn targets-route
  [rt doc name]
  (let [type (util/entity-type-name doc)
        id (:_id doc)]
    (rt (keyword (format "get-%s-link-targets" type)) {:id id :rel name})))

(defn self-route
  [rt doc]
  (let [type (util/entity-type-name doc)
        id (:_id doc)]
    (rt (keyword (format "get-%s" type)) {:id id})))

(defn named-route
  [rt name args]
  (rt (keyword name) args))

(defn heads-route
  [rt doc]
  (rt :file-head-revisions {:id (:_id doc)}))

(defn zip-activity-route
  [rt doc]
  (rt :zip-activity {:id (:_id doc)}))

(defn zip-folder-route
  [rt doc]
  (rt :zip-folder {:id (:_id doc)}))

(defn upload-complete-route
  [rt doc]
  (rt :upload-complete {:id (:_id doc)}))

(defn annotations-route
  [rt doc annotation-type]
  (rt (keyword (format "get-%s" annotation-type)) {:id (:_id doc)}))
