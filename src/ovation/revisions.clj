(ns ovation.revisions
  (:require [ovation.core :as core]
            [ovation.constants :as k]
            [slingshot.slingshot :refer [throw+]]
            [ovation.links :as links]
            [ovation.couch :as couch]
            [ovation.config :as config]
            [ovation.util :as util]
            [org.httpkit.client :as http]
            [ovation.auth :as auth]
            [clojure.string :as string]
            [ring.util.http-response :refer [unprocessable-entity!]]
            [com.climate.newrelic.trace :refer [defn-traced]]))

(defn-traced get-head-revisions
  [auth routes file-id]
  (let [db      (couch/db auth)
        result  (:value (first (couch/get-view auth db k/REVISIONS-VIEW {:startkey file-id
                                                                         :endkey   file-id
                                                                         :reduce   true
                                                                         :group    true})))
        ids     (first result)]
    (if (nil? ids)
      []
      (core/get-entities auth ids routes))))

(defn update-file-status
  [file revisions status]
  (loop [revs revisions
         f    file]
    (if-let [r (first revs)]
      (recur (rest revs) (assoc-in f [:revisions (:_id r)] {:status     status
                                                            :started-at (or (get-in f [:revisions (:_id r) :started-at])
                                                                          (get-in r [:attributes :created-at])
                                                                          (util/iso-now))}))
      f)))


(defn- create-revisions-from-file
  [auth routes file parent new-revisions]
  (let [previous     (if (nil? parent) [] (conj (get-in parent [:attributes :previous] []) (:_id parent)))
        new-revs     (map #(-> %
                            (assoc-in [:attributes :file_id] (:_id file))
                            (assoc-in [:attributes :previous] previous)) new-revisions)
        revisions    (core/create-entities auth new-revs routes)
        updated-file (update-file-status file revisions k/UPLOADING)
        links-result (links/add-links auth [updated-file] :revisions (map :_id revisions) routes :inverse-rel :file)]
    {:revisions revisions
     :links     (:links links-result)
     :updates   (:updates links-result)}))

(defn- create-revisions-from-revision
  [auth routes parent new-revisions]
  (let [files (core/get-entities auth [(get-in parent [:attributes :file_id])] routes)]
    (create-revisions-from-file auth routes (first files) parent new-revisions)))

(defn-traced create-revisions
  "Creates new Revisions. Returns result of links/add-links; you should call core/create-values and core/update-entities on the result"
  [auth routes parent new-revisions]

  (condp = (:type parent)
    k/REVISION-TYPE (create-revisions-from-revision auth routes parent new-revisions)
    k/FILE-TYPE (let [heads (get-head-revisions auth routes parent)]
                  (when (> (count heads) 1)
                    (throw+ {:type ::file-revision-conflict :message "File has multiple head revisions"}))
                  (create-revisions-from-file auth routes parent (first heads) new-revisions))))


(defn-traced make-resource
  [auth revision]
  (if-let [existing-url (get-in revision [:attributes :url])]
    {:revision revision
     :aws      {}
     :post-url existing-url}

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

(defn-traced make-resources
  "Create Rails Resources for each revision and update attributes accordingly"
  [auth revisions]
  (doall (map #(make-resource auth %) revisions)))          ;;TODO this would be much better as core.async channel


(defn-traced update-metadata
  [auth routes revision & {:keys [complete] :or {complete true}}]

  (when-not (re-find #"ovation.io" (get-in revision [:attributes :url]))
    (unprocessable-entity! {:errors {:detail "Unable to update metadata for URLs outside ovation.io/api/v1/resources"}}))

  (let [rsrc-id (last (string/split (get-in revision [:attributes :url]) #"/"))
        resp    (http/get (util/join-path [config/RESOURCES_SERVER rsrc-id "metadata"])
                          {:oauth-token (::auth/token auth)
                           :headers     {"Content-Type" "application/json"
                                         "Accept"       "application/json"}})
        file    (if complete
                  (future (core/get-entity auth (get-in revision [:attributes :file_id]) routes))
                  nil)]
    (let [body             (dissoc (util/from-json (:body @resp)) :etag) ;; Remove the :etag entry, since it's not useful to end user
          updated-revision (-> revision
                             (update-in [:attributes] merge body)
                             (assoc-in [:attributes :upload-status] k/COMPLETE))
          updates          (if file
                             [updated-revision (update-file-status @file [revision] k/COMPLETE)]
                             [updated-revision])]
      (first (filter #(= (:_id %) (:_id revision))
               (core/update-entities auth updates routes :direct true))))))


(defn-traced record-upload-failure
  [auth routes revision]
  (let [file-id          (get-in revision [:attributes :file_id])
        file             (core/get-entity auth file-id routes)
        updated-file     (update-file-status file [revision] k/ERROR)
        updated-revision (assoc-in revision [:attributes :upload-status] k/ERROR)
        updates          (core/update-entities auth [updated-revision updated-file] routes :direct true)]
    {:revision (first (filter #(= (:_id %) (:_id revision)) updates))
     :file     (first (filter #(= (:_id %) (:_id file)) updates))}))
