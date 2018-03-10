(ns ovation.storage
  (:require [ovation.db.revisions :as revisions]
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
        result      (if org0?
                      (revisions/storage-by-project-for-public-org db {:owner_id user-id})
                      (revisions/storage-by-project-for-private-org db {:organization_id org-id}))]
    (go
      (>! ch result)
      (if close?
        (close! ch)))

    ch))
