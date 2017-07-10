(ns ovation.storage
  (:require [ovation.couch :as couch]
            [ovation.constants :as k]
            [clojure.core.async :refer [go >! close!]]))


(defn get-projects-storage
  [ctx db ch & {:keys [close?] :or {close? true}}]
  [])


(defn get-organization-storage
  "Gets the provided organization file storage (usage) in bytes.
  For organization 0, returns usage for all Projects belonging to the current user.

  Conveys a seq of {:project_id, :org_id, :usage}"
  [ctx db org-id ch & {:keys [close?] :or {close? true}}]

  (if (= org-id 0)
    (get-projects-storage ctx db ch :close? close?)
    (let [view-result (couch/get-view ctx db k/REVISION-BYTES-VIEW {:startkey    [org-id]
                                                                    :endkey      [org-id, {}]
                                                                    :reduce      true
                                                                    :group_level 2}
                        :prefix-teams false)
          result      (map (fn [row]
                             (let [[org proj] (:key row)
                                   usage (:value row)]
                               {:organization_id org
                                :id              proj
                                :usage           usage})) view-result)]
      (go
        (>! ch result)
        (if close?
          (close! ch)))))

  ch)
