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

(defn-traced create-revisions
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
  [auth routes revision]

  (when-not (re-find #"ovation.io" (get-in revision [:attributes :url]))
    (unprocessable-entity! {:errors {:detail "Unable to update metadata for URLs outside ovation.io/api/v1/resources"}}))

  (let [rsrc-id          (last (string/split (get-in revision [:attributes :url]) #"/"))
        resp             (http/get (util/join-path [config/RESOURCES_SERVER rsrc-id "metadata"])
                          {:oauth-token (::auth/token auth)
                           :headers     {"Content-Type" "application/json"
                                         "Accept"       "application/json"}})
        body             (dissoc (util/from-json (:body @resp)) :etag) ;; Remove the :etag entry, since it's not useful to end user
        updated-revision (update-in revision [:attributes] merge body)]
    (first (core/update-entities auth [updated-revision] routes))))

