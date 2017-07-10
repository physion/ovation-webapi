(ns ovation.storage
  (:require [ovation.couch :as couch]
            [ovation.constants :as k]
            [clojure.core.async :refer [go >! close!]]
            [ovation.request-context :as request-context]))



(defn get-organization-storage
  "Gets the provided organization file storage (usage) in bytes.
  For organization 0, returns usage for all Projects belonging to the current user.

  Conveys a seq of {:project_id, :org_id, :usage}"
  [ctx db org-id ch & {:keys [close?] :or {close? true}}]

  (let [org0?       (= 0 org-id)
        user-id     (request-context/user-id ctx)
        startkey    (if org0? [user-id] [org-id])
        endkey      (if org0? [user-id {}] [org-id, {}])
        view-result (couch/get-view ctx db k/REVISION-BYTES-VIEW {:startkey    startkey
                                                                  :endkey      endkey
                                                                  :reduce      true
                                                                  :group_level 2}
                      :prefix-teams org0?)

        tf          (fn [row]
                      (let [[org proj] (:key row)
                            usage (:value row)]
                        {:organization_id org
                         :id              proj
                         :usage           usage}))

        result      (if org0?
                      (mapcat (fn [r]
                                (let [rows (:rows r)]
                                  (map tf rows))) view-result)
                      (map tf view-result))]
    (go
      (>! ch result)
      (if close?
        (close! ch)))

    ch))
