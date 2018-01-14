(ns ovation.route-helpers
  (:require [clojure.string :refer [lower-case capitalize upper-case join]]
            [clojure.tools.logging :as logging]
            [clojure.walk :as walk]
            [com.climate.newrelic.trace :refer [defn-traced]]
            [compojure.api.sweet :refer :all]
            [ovation.annotations :as annotations]
            [ovation.auth :as auth]
            [ovation.constants :as k]
            [ovation.core :as core]
            [ovation.db.folders :as folders]
            [ovation.links :as links]
            [ovation.request-context :as request-context]
            [ovation.revisions :as revisions]
            [ovation.routes :as r]
            [ovation.routes :as routes]
            [ovation.schema :refer :all]
            [ovation.teams :as teams]
            [ovation.transform.serialize :as serialize]
            [ovation.util :as util]
            [ring.util.http-response :refer [created ok accepted not-found not-found! unauthorized bad-request bad-request! conflict forbidden unprocessable-entity!]]
            [schema.core :as s]
            [slingshot.slingshot :refer [try+ throw+]]))


(defn-traced get-annotations*
  [ctx db id annotation-key]
  (let [annotations (annotations/get-annotations ctx db [id] annotation-key)]
    (ok {(keyword annotation-key) (serialize/values annotations)})))

(defn-traced post-annotations*
  [ctx db id annotation-key annotations]
  (let [annotations-kw (keyword annotation-key)]
    (created (routes/entity-route ctx id)
      {annotations-kw (serialize/values (annotations/create-annotations ctx db [id] annotation-key annotations))})))

(defn-traced delete-annotations*
  [ctx db annotation-id annotation-key]
  (accepted {(keyword annotation-key) (serialize/values (annotations/delete-annotations ctx db [annotation-id]))}))

(defn-traced put-annotation*
  [ctx db annotation-key annotation-id annotation]

  (ok {(keyword annotation-key) (serialize/value (annotations/update-annotation ctx db annotation-id annotation))}))


(defn annotation
  "Creates an annotation type endpoint"
  [db org authz id annotation-description annotation-key record-schema annotation-schema]

  (let [annotation-kw (keyword annotation-key)]
    (context (str "/" annotation-key) []
      :tags ["annotations"]
      (GET "/" request
        :name (keyword (str "get-" (lower-case annotation-key)))
        :return {annotation-kw [annotation-schema]}
        :summary (str "Returns all " annotation-description " annotations associated with entity :id")
        (get-annotations* (request-context/make-context request org authz) db id annotation-key))

      (POST "/" request
        :name (keyword (str "create-" (lower-case annotation-key)))
        :return {(keyword annotation-key) [annotation-schema]}
        :body [new-annotations {(keyword annotation-key) [record-schema]}]
        :summary (str "Adds a new " annotation-description " annotation to entity :id")
        (post-annotations* (request-context/make-context request org authz) db id annotation-key ((keyword annotation-key) new-annotations)))

      ;(if (= annotation-kw :notes))
      (context "/:annotation-id" []
        :path-params [annotation-id :- s/Str]
        (DELETE "/" request
          :name (keyword (str "delete-" (lower-case annotation-key)))
          :return [s/Str]
          :summary (str "Removes a " annotation-description " annotation from entity :id. Returns the deleted annotations.")
          (delete-annotations* (request-context/make-context request org authz) db annotation-id annotation-key))

        (if (= annotation-kw :notes)
          (PUT "/" request
            :name (keyword (str "update-" (lower-case annotation-key)))
            :return {:note annotation-schema}
            :body [update {:note record-schema}]
            :summary (str "Updates a " annotation-description " annotation to entity :id.")
            (put-annotation* (request-context/make-context request org authz) db "note" annotation-id (:note update))))))))



(defn- typepath
  [typename]
  (case (lower-case typename)
    "activity" "activities"
    ;;default
    (lower-case (str typename "s"))))

(defn get-resources
  "Get all resources of type (e.g. \"Project\")"
  [db org authz entity-type record-schema]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)
        type-kw   (keyword type-path)]
    (GET "/" request
      :name (keyword (str "all-" (lower-case type-name)))
      :return {type-kw [record-schema]}
      :summary (str "Gets all top-level " type-path)
      (let [ctx      (request-context/make-context request org authz)
            entities (core/of-type ctx db type-name)]
        (ok {type-kw (serialize/entities entities)})))))

(defn remove-embedded-relationships
  [entities]
  (map #(dissoc % :relationships) entities))

(defn- relationships-map
  [pre post]
  (into {} (remove #(nil? (second %)) (map (fn [e-pre e-post] [(:_id e-post) (:relationships e-pre)]) pre post))))


(defn- embedded-links
  [ctx db entities rel-map]
  (let [{org ::request-context/org} ctx
        entities-map (util/into-id-map entities)]
    (mapcat (fn [[id relationships]]
              (map (fn [[rel info]]
                     (if (:create_as_inverse info)
                       (links/add-links ctx db (core/get-entities ctx db (:related info)) (:inverse_rel info) [id] :inverse-rel rel)
                       (links/add-links ctx db [(get entities-map id)] rel (:related info) :inverse-rel (:inverse_rel info))))
                relationships))
      rel-map)))

(defn post-resources*
  [ctx db type-name type-kw new-entities]
  (let [{routes ::request-context/routes
         org    ::request-context/org} ctx]
    (if (every? #(= type-name (:type %)) new-entities)
      (try+
        (let [cleaned-entities (remove-embedded-relationships new-entities)
              created-entities (core/create-entities ctx db cleaned-entities)
              entities         (if (some :relationships new-entities)
                                 (core/get-entities ctx db (map :_id created-entities))
                                 created-entities)
              rel-map          (relationships-map new-entities entities) ;; NB Assumes result of create-entities is in the same order as body!!
              embedded-links   (embedded-links ctx db entities rel-map)
              links            (core/create-values ctx db (mapcat :links embedded-links)) ;; Combine all :links from child and embedded
              updates          (core/update-entities ctx db (mapcat :updates embedded-links) :authorize false :update-collaboration-roots true)]
          ;; create teams for new Project entities
          ;; (dorun (map #(teams/create-team ctx (:_id %)) (filter #(= (:type %) k/PROJECT-TYPE) entities)))

          (if (and (zero? (count links))
                (zero? (count updates)))
            (created (routes (keyword (format "all-%s" (lower-case type-name))) {:org org}) {type-kw (serialize/entities entities)})
            (created (routes (keyword (format "all-%s" (lower-case type-name))) {:org org}) {type-kw (serialize/entities entities)
                                                                                             :links   (serialize/values links)
                                                                                             :updates (serialize/entities updates)})))

        (catch [:type :ovation.auth/unauthorized] _
          (unauthorized {:errors {:detail "Not authorized"}})))

      (bad-request {:errors {:detail (str "Entities must be of \"type\" " type-name)}}))))

(defmacro post-resources
  "POST to resources of type (e.g. \"Project\")"
  [db org authz entity-type schemas]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)
        type-kw (keyword type-path)]
    `(POST "/" request#
       :return {~type-kw                  [~(clojure.core/symbol "ovation.schema" type-name)]
                (s/optional-key :links)   [LinkInfo]
                (s/optional-key :updates) [Entity]}
       :body [entities# {~type-kw [(apply s/either ~schemas)]}]
       :summary ~(str "Creates a new top-level " type-name)
       (let [ctx# (request-context/make-context request# ~org ~authz)]
         (post-resources* ctx# ~db ~type-name ~type-kw (~type-kw entities#))))))

(defmacro get-resource
  [db org authz entity-type id]
  (let [type-name (capitalize entity-type)
        single-type-kw (keyword (lower-case type-name))]
    `(GET "/" request#
       :name ~(keyword (str "get-" (lower-case type-name)))
       :return {~single-type-kw ~(clojure.core/symbol "ovation.schema" type-name)}
       :summary ~(str "Returns " type-name " with :id")
       (let [ctx# (request-context/make-context request# ~org ~authz)]
         (if-let [entities# (core/get-entities ctx# ~db [~id])]
           (if-let [filtered# (seq (filter #(= ~type-name (:type %)) entities#))]
             (ok {~single-type-kw (serialize/entity (first filtered#))})
             (not-found {:errors {:detail "Not found"}})))))))

(defn make-child-link*
  [ctx db sources target-ids source-type]
  (fn [target]
    (let [target-type (util/entity-type-keyword target)
          rel (get-in EntityChildren [source-type target-type :rel])
          inverse-rel (get-in EntityChildren [source-type target-type :inverse-rel])]

      (if rel
        (links/add-links ctx db sources rel target-ids :inverse-rel inverse-rel)
        {}))))

(defn make-child-links*
  [ctx db parent-id type-name targets]
  (let [target-ids (map :_id targets)
        sources    (core/get-entities ctx db [parent-id])
        type       (util/entity-type-name-keyword type-name)
        results    (map (make-child-link* ctx db sources target-ids type) targets)
        links      (mapcat :links results)
        updates    (mapcat :updates results)]
    {:links links
     :updates updates}))


(defn post-resource*
  [ctx db type-name id body]

  (try+

    (let [cleaned-entities (remove-embedded-relationships body)
          entities         (core/create-entities ctx db cleaned-entities :parent id)
          rel-map          (relationships-map body entities) ;; NB Assumes result of create-entities is in the same order as body!!
          child-links      (make-child-links* ctx db id type-name entities)
          embedded-links   (embedded-links ctx db entities rel-map)
          links            (core/create-values ctx db (mapcat :links (cons child-links embedded-links))) ;; Combine all :links from child and embedded
          updates          (core/update-entities ctx db (mapcat :updates (cons child-links embedded-links)) :authorize false :update-collaboration-roots true)] ;; Combine all :updates from child and embedded links

      (created (routes/self-route ctx (first entities)) {:entities (serialize/entities entities)
                                                         :links    (serialize/values links)
                                                         :updates  (serialize/entities updates)}))

    (catch [:type :ovation.auth/unauthorized] err
      (unauthorized {:errors {:detail "Not authorized to create new entities"}}))))


(defmacro post-resource
  [db org authz entity-type id schemas]
  (let [type-name (capitalize entity-type)]
    `(POST "/" request#
       :name ~(keyword (format "create-%s-entity" (lower-case type-name)))
       :return {:entities [(apply s/either ~schemas)]
                :links    [LinkInfo]
                :updates  [Entity]}
       :body [body# {:entities [(apply s/either ~schemas)]}]
       :summary ~(str "Creates and returns a new entity with the identified " type-name " as collaboration root")
       (let [ctx# (request-context/make-context request# ~org ~authz)]
         (post-resource* ctx# ~db ~type-name ~id (:entities body#))))))


(defn-traced put-resource*
  [ctx db id type-name type-kw updates]
  (let [entity-id (str (:_id updates))]
    (if-not (= id (str entity-id))
      (not-found {:error (str type-name " " entity-id " ID mismatch")})
      (try+
        (let [entity (first (core/update-entities ctx db [updates]))]
          (ok {type-kw (serialize/entity entity)}))

        (catch [:type :ovation.auth/unauthorized] _
          (unauthorized {:errors {:detail "Unauthorized"}}))))))

(defmacro put-resource
  [db org authz entity-type id]
  (let [type-name (capitalize entity-type)
        update-type (format "%sUpdate" type-name)
        type-kw (util/entity-type-name-keyword type-name)]
    `(PUT "/" request#
       :name ~(keyword (str "update-" (lower-case type-name)))
       :return {~type-kw ~(clojure.core/symbol "ovation.schema" type-name)}
       :body [updates# {~type-kw ~(clojure.core/symbol "ovation.schema" update-type)}]
       :summary ~(str "Updates and returns " type-name " with :id")
       (let [ctx# (request-context/make-context request# ~org ~authz)]
         (put-resource* ctx# ~db ~id ~type-name ~type-kw (~type-kw updates#))))))

(defmacro delete-resource
  [db org authz entity-type id]
  (let [type-name (capitalize entity-type)]

    `(DELETE "/" request#
       :name ~(keyword (str "delete-" (lower-case type-name)))
       :return {:entity TrashedEntity}
       :summary ~(str "Deletes (trashes) " type-name " with :id")
       (try+
         (let [ctx# (request-context/make-context request# ~org ~authz)]
           (accepted {:entity (serialize/entity (first (core/delete-entities ctx# ~db [~id])))}))
         (catch [:type :ovation.auth/unauthorized] err#
           (unauthorized {}))))))

(defn-traced rel-related*
  [ctx db id rel]
  (let [related (links/get-link-targets ctx db id (lower-case rel))]
    (ok {(keyword rel) (serialize/entities related)})))


(defmacro rel-related
  [db org authz entity-type id rel]
  (let [type-name (capitalize entity-type)]
    `(GET "/" request#
       :name ~(keyword (str "get-" (lower-case type-name) "-link-targets"))
       :return {s/Keyword [Entity]}
       :summary ~(str "Gets the targets of relationship :rel from the identified " type-name)
       (let [ctx# (request-context/make-context request# ~org ~authz)]
         (rel-related* ctx# ~db ~id ~rel)))))

(defn-traced get-relationships*
  [ctx db id rel]
  (logging/info "get-relationships* for " id rel)
  (let [rels (links/get-links ctx db id rel)]
    (logging/info "found " rels)
    (ok {:links (serialize/values rels)})))


(defn-traced post-relationships*
  [ctx db id new-links rel]
  (try+
    (let [source (first (core/get-entities ctx db [id]))]
      (when source
        (auth/check! ctx ::auth/update source))
      (if source
        (let [groups      (group-by :inverse_rel new-links)
              link-groups (map (fn [[irel nlinks]] (links/add-links ctx db [source] rel (map :target_id nlinks) :inverse-rel irel)) (seq groups))]
          (let [links   (core/create-values ctx db (flatten (map :links link-groups)))
                updates (core/update-entities ctx db (flatten (map :updates link-groups)) :authorize false :update-collaboration-roots true)]
            (created (routes/entity-route ctx id) {:updates (serialize/entities updates)
                                                   :links   (serialize/values links)})))
        (not-found {:errors {:detail (str ~id " not found")}})))
    (catch [:type :ovation.auth/unauthorized] {:keys [message]}
      (unauthorized {:errors {:detail message}}))
    (catch [:type :ovation.links/target-not-found] {:keys [message]}
      (bad-request {:errors {:detail message}}))))

(defmacro relationships
  [db org authz entity-type id rel]
  (let [type-name (capitalize entity-type)]
    `(context "/relationships" []
       (GET "/" request#
         :name ~(keyword (str "get-" (lower-case type-name) "-links"))
         :return {:links [LinkInfo]}
         :summary ~(str "Get relationships for :rel from " type-name " :id")
         (let [ctx# (request-context/make-context request# ~org ~authz)]
           (get-relationships* ctx# ~db ~id ~rel)))

       (POST "/" request#
         :name ~(keyword (str "create-" (lower-case type-name) "-links"))
         :return {:links   [LinkInfo]
                  :updates [Entity]}
         :body [new-links# [NewLink]]
         :summary ~(str "Add relationship links for :rel from " type-name " :id")
         (let [ctx# (request-context/make-context request# ~org ~authz)]
           (post-relationships* ctx# ~db ~id new-links# ~rel))))))

(defn-traced post-revisions*
  [ctx db id revisions]
  (try+
    (let [parent                   (core/get-entity ctx db id)
          revisions-with-ids       (map #(assoc % :_id (str (util/make-uuid))) revisions)
          revisions-with-resources (revisions/make-resources ctx revisions-with-ids)
          result                   (revisions/create-revisions ctx db parent (map :revision revisions-with-resources))
          links                    (core/create-values ctx db (:links result))
          updates                  (core/update-entities ctx db (:updates result)
                                     :update-collaboration-roots true
                                     :allow-keys [:revisions])]

      {:entities (serialize/entities (filter #(= (:type %) k/REVISION-TYPE) updates))
       :links    (serialize/values links)
       :updates  (serialize/entities updates)
       :aws      (map (fn [m] {:id  (get-in m [:revision :_id])
                               :aws (walk/keywordize-keys (:aws m))}) revisions-with-resources)})
    (catch [:type :ovation.revisions/file-revision-conflict] err
      (conflict {:errors {:detail (:message err)}}))))

(defn-traced get-head-revisions*
  [request db org authz id]
  (let [ctx (request-context/make-context request org nil)]

    (try+
      (ok {:revisions (serialize/entities (revisions/get-head-revisions ctx db id))})
      (catch [:type ::revisions/not-found] _
        (not-found! {:errors {:detail "File not found"}})))))

(defn- rel
  [src dest]
  (get-in EntityChildren [(util/entity-type-keyword src) (util/entity-type-keyword dest) :rel]))

(defn inverse-rel
  [src dest]
  (get-in EntityChildren [(util/entity-type-keyword src) (util/entity-type-keyword dest) :inverse-rel]))

(defn-traced move-contents*
  [request db org authz id info]
  (let [ctx    (request-context/make-context request org authz)

        src    (core/get-entities ctx db [(:source info)])
        dest   (core/get-entities ctx db [(:destination info)])
        entity (first (core/get-entities ctx db [id]))]

    (if (and
          (contains? #{k/FILE-TYPE k/FOLDER-TYPE} (:type entity))
          (contains? #{k/FOLDER-TYPE k/PROJECT-TYPE} (:type (first src)))
          (contains? #{k/FOLDER-TYPE k/PROJECT-TYPE} (:type (first dest))))

      (let [added   (links/add-links ctx db dest (rel (first dest) entity) [id] :inverse-rel (inverse-rel (first dest) entity))
            links   (core/create-values ctx db (:links added))
            updates (core/update-entities ctx db (:updates added) :authorize false :update-collaboration-roots true)]

        (do
          (if (= (:type entity) k/FOLDER-TYPE)
            (folders/update-project-id db (assoc entity :project_id (:project_id (first dest)))))
          (links/delete-links ctx db(first src) (rel (first src) entity) id)

          {(util/entity-type-keyword entity)    entity
           :updates updates
           :links   links}))

      (unprocessable-entity! {:errors {:detail "Unexpected entity type"}}))))
