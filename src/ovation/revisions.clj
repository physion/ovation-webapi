(ns ovation.revisions
  (:require [clojure.java.jdbc :as jdbc]
            [ovation.core :as core]
            [ovation.constants :as k]
            [slingshot.slingshot :refer [throw+]]
            [ovation.links :as links]
            [ovation.db.files :as files]
            [ovation.db.revisions :as revisions]
            [ovation.config :as config]
            [ovation.util :as util]
            [org.httpkit.client :as http]
            [ovation.auth :as auth]
            [clojure.string :as string]
            [ring.util.http-response :refer [unprocessable-entity!]]
            [com.climate.newrelic.trace :refer [defn-traced]]
            [ovation.transform.read :as tr]
            [clojure.tools.logging :as logging]
            [ovation.request-context :as request-context]
            [ovation.pubsub :as pubsub]
            [clojure.core.async :as async]))

(defn-traced get-head-revisions
  "Gets HEAD revisions for the given file-id. Queries revisions view for top 2 parent lengths. If
  they're not equal (or if there are less than 2), returns the top doc. If they're equal, returns
  all Revisions with that parent count"

  [ctx db file-id]
  (let [{org-id ::request-context/org
         auth ::request-context/identity} ctx
        teams (auth/authenticated-teams auth)
        docs (revisions/find-head-by-file-id db {:organization_id org-id
                                                 :team_uuids (if (empty? teams) [nil] teams)
                                                 :service_account (auth/service-account auth)
                                                 :file_id file-id})]
    (-> docs
      (tr/entities-from-db ctx))))

(defn update-file-status
  [file revisions status]
  (loop [revs revisions
         f    file]
    (if-let [r (first revs)]
      (recur (rest revs) (assoc-in f [:revisions (:_id r)] {:status     (if (get-in r [:attributes :remote]) k/COMPLETE status)
                                                            :started-at (or (get-in f [:revisions (:_id r) :started-at])
                                                                          (get-in r [:attributes :created-at])
                                                                          (util/iso-now))}))
      f)))


(defn- create-revisions-from-file
  [ctx db file parent new-revisions]
  (jdbc/with-db-transaction [tx db]
    (let [previous     (if (nil? parent) [] (conj (get-in parent [:attributes :previous] []) (:_id parent)))
          new-revs     (map #(-> %
                               (assoc-in [:attributes :file_id] (:_id file))
                               (assoc-in [:attributes :previous] previous)) new-revisions)
          revisions    (core/create-entities ctx tx new-revs)
          updated-file (update-file-status file revisions k/UPLOADING) ;; only if uploading, save
          _ (files/update-head-revision tx {:_id (:_id file)
                                            :organization_id (:organization_id file)
                                            :head_revision_id (:id (first revisions))
                                            :updated-at (util/iso-now)})
          links-result (links/add-links ctx tx [updated-file] :revisions (map :_id revisions) :inverse-rel :file)]
      {:revisions revisions
       :links     (:links links-result)
       :updates   (:updates links-result)})))

(defn- create-revisions-from-revision
  [ctx db parent new-revisions]
  (let [files (core/get-entities ctx db [(get-in parent [:attributes :file_id])])]
    (create-revisions-from-file ctx db (first files) parent new-revisions)))

(defn-traced create-revisions
  "Creates new Revisions. Returns result of links/add-links; you should call core/create-values and core/update-entities on the result"
  [ctx db parent new-revisions]

  (condp = (:type parent)
    k/REVISION-TYPE (create-revisions-from-revision ctx db parent new-revisions)
    k/FILE-TYPE (let [heads (get-head-revisions ctx db parent)]
                  (when (> (count heads) 1)
                    (throw+ {:type ::file-revision-conflict :message "File has multiple head revisions"}))
                  (create-revisions-from-file ctx db parent (first heads) new-revisions))))


(defn-traced make-resource
  [ctx revision]
  (if-let [existing-url (get-in revision [:attributes :url])]
    {:revision (-> revision
                 (assoc-in [:attributes :remote] true)
                 (assoc-in [:attributes :upload-status] k/COMPLETE))
     :aws      {}
     :post-url existing-url}

    (let [body {:entity_id (:_id revision)
                :path      (get-in revision [:attributes :name] (:_id revision))}
          resp (http/post (util/join-path [config/RESOURCES_SERVER "api" "v1" "resources"]) {:oauth-token (request-context/token ctx)
                                                   :body                                                  (util/to-json body)
                                                   :headers                                               {"Content-Type" "application/json"}})]
      (when-not (= (:status @resp) 201)
        (throw+ {:type ::resource-creation-failed :message (util/from-json (:body @resp)) :status (:status @resp)}))

      (logging/info "POST /resources => " (:body @resp))
      (let [result   (:resource (util/from-json (:body @resp)))
            id       (:id result)
            url      (:public_url result)
            aws      (:aws result)
            post-url (:url result)]
        {:revision (-> revision
                     (assoc-in [:attributes :resource_id] id)
                     (assoc-in [:attributes :url] url)
                     (assoc-in [:attributes :upload-status] k/UPLOADING))
         :aws      aws
         :post-url post-url}))))

(defn-traced make-resources
  "Create Rails Resources for each revision and update attributes accordingly"
  [ctx revisions]
  (doall (map #(make-resource ctx %) revisions)))           ;;TODO this would be much better as core.async channel


(defn publish-revision
  [db doc]
  (let [publisher (get-in db [:pubsub :publisher])
        topic     (config/config :revisions-topic :default :revisions)]
    (pubsub/publish publisher topic {:id   (:_id doc)
                                     :type (:type doc)} (async/chan))))
(defn update-metadata
  [ctx db revision & {:keys [complete] :or {complete true}}]

  (if (re-find #"ovation.io" (get-in revision [:attributes :url]))
    ;; /api/v1/resources Resource
    (let [rsrc-id (last (string/split (get-in revision [:attributes :url]) #"/"))
          resp    (http/get (util/join-path [config/RESOURCES_SERVER "api" "v1" "resources" rsrc-id "metadata"])
                    {:oauth-token (request-context/token ctx)
                     :headers     {"Content-Type" "application/json"
                                   "Accept"       "application/json"}})
          file    (if complete
                    (core/get-entity ctx db (get-in revision [:attributes :file_id]))
                    nil)]
      (let [body             (if (= (:status @resp) 200)
                               (dissoc (util/from-json (:body @resp)) :etag) ;; Remove the :etag entry, since it's not useful to end user
                               {})                            ;; if it's not 200, don't add any additional attributes
            updated-revision (-> revision
                               (update-in [:attributes] merge body)
                               (assoc-in [:attributes :upload-status] k/COMPLETE))
            updates          (if file
                               [updated-revision (update-file-status file [revision] k/COMPLETE)]
                               [updated-revision])
            update-result    (first (filter #(= (:_id %) (:_id revision))
                                      (core/update-entities ctx db updates :allow-keys [:revisions])))]

        ;;(publish-revision db update-result)
        update-result))

    ;; Remote resource
    (let [file (if complete
                    (core/get-entity ctx db (get-in revision [:attributes :file_id]))
                    nil)]
      (logging/info "Updating file status for remote revision")
      (when file
        (let [updated-revision (-> revision
                                 (assoc-in [:attributes :upload-status] k/COMPLETE))
              updates          [updated-revision (update-file-status file [revision] k/COMPLETE)]
              update-result    (first (filter #(= (:_id %) (:_id revision))
                                        (core/update-entities ctx db updates :allow-keys [:revisions])))]

          ;;(publish-revision db update-result)
          update-result)))))


(defn-traced record-upload-failure
  [ctx db revision]
  (let [file-id          (get-in revision [:attributes :file_id])
        file             (core/get-entity ctx db file-id)
        updated-file     (update-file-status file [revision] k/ERROR)
        updated-revision (assoc-in revision [:attributes :upload-status] k/ERROR)
        updates          (core/update-entities ctx db [updated-revision updated-file] :allow-keys [:revisions])]
    {:revision (first (filter #(= (:_id %) (:_id revision)) updates))
     :file     (first (filter #(= (:_id %) (:_id file)) updates))}))
