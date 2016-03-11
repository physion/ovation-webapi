(ns ovation.revisions
  (:require [ovation.core :as core]
            [ovation.constants :as k]
            [slingshot.slingshot :refer [throw+]]
            [ovation.links :as links]
            [ovation.couch :as couch]
            [ovation.config :as config]
            [ovation.util :as util]
            [org.httpkit.client :as http]
            [ovation.auth :as auth]))

(defn get-head-revisions
  [auth routes file]
  (let [db      (couch/db auth)
        file-id (:_id file)
        result  (:value (first (couch/get-view auth db k/REVISIONS-VIEW {:startkey file-id
                                                                         :endkey   file-id
                                                                         :reduce   true
                                                                         :group    true})))
        ids     (first result)]
    (if (nil? ids)
      []
      (core/get-entities auth ids routes))))

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


(defn make-resource
  [auth revision]
  (if (:url revision)
    {:revision revision
     :aws      {}
     :post-url (:url revision)}

    (let [body {:entity_id (:_id revision)
                :path      (get-in revision [:attributes :name] (:_id revision))}
          resp (http/post config/RESOURCES_SERVER {:oauth-token (::auth/token auth)
                                                   :body        (util/to-json body)
                                                   :headers     {"Content-Type" "application/json"}})]
      (when-not (= (:status @resp) 201)
        (throw+ {:type ::resource-creation-failed :message (util/from-json (:body @resp)) :status (:status @resp)}))

      (let [result   (:resource (util/from-json (:body @resp)))
            url (:public_url result)
            aws (:aws result)
            post-url (:url result)]
        {:revision (assoc-in revision [:attributes :url] url)
         :aws      aws
         :post-url post-url}))))

(defn make-resources
  "Create Rails Resources for each revision and update attributes accordingly"
  [auth revisions]
  (doall (map #(make-resource auth %) revisions)))          ;;TODO this would be much better as core.async channel
