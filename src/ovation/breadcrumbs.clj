(ns ovation.breadcrumbs
  (:require [ovation.links :as links]
            [ovation.constants :as k]
            [ubergraph.core :as uber]))


(defn get-parents
  [auth routes doc]
  (links/get-link-targets auth (:_id doc) k/PARENTS-REL routes))

(defn get-breadcrumbs
  "Gets all breadcrumb paths to entities with IDs `ids`"
  [auth routes ids]
  (let [g (uber/digraph)]
    g))


