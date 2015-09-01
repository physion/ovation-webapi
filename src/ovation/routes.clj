(ns ovation.routes
  (:require [compojure.api.routes :refer [path-for*]]))

(defn router
  [request]
  (fn [name]
    (path-for* name request)))

(defn relationship-route [rt doc name]
  (let [type (:type doc)]
    (rt (keyword (format "%s-relationships-%s" type name)))))

(defn targets-route [rt doc name]
  (let [type (:type doc)]
    (rt (keyword (format "%s-%s" type name)))))

(defn self-route [rt doc]
  (let [type (:type doc)]
    (rt (keyword type))))

(defn annotations-route
  [rt doc]
  (rt (keyword (format "%s-attributes" (:type doc)))))
