(ns ovation.revisions
  (:require [ovation.core :as core]
            [ovation.constants :as k]
            [slingshot.slingshot :refer [throw+]]
            [ovation.links :as links]
            [ovation.couch :as couch]))

(defn get-head-revisions
  [auth routes file]
  (let [db (couch/db auth)
        file-id (:_id file)
        result (:value (first (:rows (couch/get-view db k/REVISIONS-VIEW {:startkey file-id
                                                                          :endkey   file-id
                                                                          :reduce   true
                                                                          :group    true}))))
        ids (first result)]
    (core/get-entities auth ids routes)))

(defn- create-revisions-from-file
  [auth routes file parent new-revisions]
  (let [previous (if (nil? parent) [] (conj (get-in parent [:attributes :previous] []) (:_id parent)))
        new-revs (map #(-> %
                        (assoc-in [:attributes :file_id] (:_id file))
                        (assoc-in [:attributes :previous] previous)) new-revisions)
        revisions (core/create-entities auth new-revs routes)
        links-result (links/add-links auth [file] :revisions (map :_id revisions) routes :inverse-rel :file)]
    {:revisions revisions
     :links     (:links links-result)
     :updates   (:updates links-result)}))

(defn- create-revisions-from-revision
  [auth routes parent new-revisions]
  (let [files (core/get-entities auth [(get-in parent [:attributes :file_id])] routes)]
    (create-revisions-from-file auth routes (first files) parent new-revisions)))

(defn create-revisions
  [auth routes parent new-revisions]

  (condp = (:type parent)
    k/REVISION-TYPE (create-revisions-from-revision auth routes parent new-revisions)
    k/FILE-TYPE (let [heads (get-head-revisions auth routes parent)]
                  (when (> (count heads) 1)
                    (throw+ {:type ::file-revision-conflict :message "File has multiple head revisions"}))
                  (create-revisions-from-file auth routes parent (first heads) new-revisions))))
