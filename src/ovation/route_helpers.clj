(ns ovation.route-helpers
  (:require [compojure.api.sweet :refer :all]
            [ovation.annotations :as annotations]
            [schema.core :as s]
            [ring.util.http-response :refer [created ok accepted not-found not-found! unauthorized bad-request bad-request! conflict forbidden unprocessable-entity!]]
            [ovation.core :as core]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.string :refer [lower-case capitalize upper-case join]]
            [ovation.schema :refer :all]
            [ovation.links :as links]
            [ovation.util :as util]
            [ovation.routes :as r]
            [ovation.auth :as auth]
            [ovation.revisions :as revisions]
            [clojure.walk :as walk]
            [ovation.constants :as k]
            [ovation.teams :as teams]
            [ovation.routes :as routes]
            [com.climate.newrelic.trace :refer [defn-traced]]))


(defn-traced get-annotations*
  [request id annotation-key]
  (let [auth (auth/identity request)
        rt (routes/router request)
        annotations (annotations/get-annotations auth [id] annotation-key rt)]
    (ok {(keyword annotation-key) annotations})))

(defn-traced post-annotations*
  [request id annotation-key annotations]
  (let [auth (auth/identity request)
        annotations-kw (keyword annotation-key)]
    (created {annotations-kw (annotations/create-annotations auth (r/router request) [id] annotation-key annotations)})))

(defn-traced delete-annotations*
  [request annotation-id annotation-key]
  (let [auth (auth/identity request)]
    (accepted {(keyword annotation-key) (annotations/delete-annotations auth [annotation-id] (r/router request))})))

(defn-traced put-annotation*
  [request annotation-key annotation-id annotation]

  (let [auth (auth/identity request)]
    (ok {(keyword annotation-key) (annotations/update-annotation auth (r/router request) annotation-id annotation)})))


(defn annotation
  "Creates an annotation type endpoint"
  [id annotation-description annotation-key record-schema annotation-schema]

  (let [annotation-kw (keyword annotation-key)]
    (context (str "/" annotation-key) []
       :tags ["annotations"]
       (GET "/" request
         :name (keyword (str "get-" (lower-case annotation-key)))
         :return {annotation-kw [annotation-schema]}
         :summary (str "Returns all " annotation-description " annotations associated with entity :id")
         (get-annotations* request id annotation-key))

       (POST "/" request
         :name (keyword (str "create-" (lower-case annotation-key)))
         :return {(keyword annotation-key) [annotation-schema]}
         :body [new-annotations {(keyword annotation-key) [record-schema]}]
         :summary (str "Adds a new " annotation-description " annotation to entity :id")
         (post-annotations* request id annotation-key ((keyword annotation-key) new-annotations)))

      ;(if (= annotation-kw :notes))
      (context "/:annotation-id" []
        :path-params [annotation-id :- s/Str]
        (DELETE "/" request
          :name (keyword (str "delete-" (lower-case annotation-key)))
          :return [s/Str]
          :summary (str "Removes a " annotation-description " annotation from entity :id. Returns the deleted annotations.")
          (delete-annotations* request annotation-id annotation-key))

        (if (= annotation-kw :notes)
          (PUT "/" request
            :name (keyword (str "update-" (lower-case annotation-key)))
            :return {:note annotation-schema}
            :body [update {:note record-schema}]
            :summary (str "Updates a " annotation-description " annotation to entity :id.")
            (put-annotation* request "note" annotation-id (:note update))))))))



(defn- typepath
  [typename]
  (case (lower-case typename)
    "activity" "activities"
    ;;default
    (lower-case (str typename "s"))))

(defmacro get-resources
  "Get all resources of type (e.g. \"Project\")"
  [entity-type]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)
        type-kw   (keyword type-path)]
    `(GET "/" request#
       :name ~(keyword (str "all-" (lower-case type-name)))
       :return {~type-kw [~(clojure.core/symbol "ovation.schema" type-name)]}
       :summary (str "Gets all top-level " ~type-path)
       (let [auth# (auth/identity request#)
             entities# (core/of-type auth# ~type-name (r/router request#))]
         (ok {~type-kw entities#})))))

(defn remove-embedded-relationships
  [entities]
  (map #(dissoc % :relationships) entities))

(defn- relationships-map
  [pre post]
  (into {} (remove #(nil? (second %)) (map (fn [e-pre e-post] [(:_id e-post) (:relationships e-pre)]) pre post))))


(defn- embedded-links
  [auth entities rel-map routes]
  (let [entities-map (util/into-id-map entities)]
    (mapcat (fn [[id relationships]]
              (map (fn [[rel info]]
                     (if (:create_as_inverse info)
                       (links/add-links auth (core/get-entities auth (:related info) routes) (:inverse_rel info) [id] routes :inverse-rel rel)
                       (links/add-links auth [(get entities-map id)] rel (:related info) routes :inverse-rel (:inverse_rel info))))
                relationships))
      rel-map)))

(defn-traced post-resources*
  [request type-name type-kw new-entities]
  (let [auth (auth/identity request)]
    (if (every? #(= type-name (:type %)) new-entities)
      (try+
        (let [routes           (r/router request)
              cleaned-entities (remove-embedded-relationships new-entities)
              entities         (core/create-entities auth cleaned-entities routes)
              rel-map          (relationships-map new-entities entities) ;; NB Assumes result of create-entities is in the same order as body!!
              embedded-links   (embedded-links auth entities rel-map routes)
              links            (core/create-values auth routes (mapcat :links embedded-links)) ;; Combine all :links from child and embedded
              updates          (core/update-entities auth (mapcat :updates embedded-links) routes :authorize false :update-collaboration-roots true)]
          ;; create teams for new Project entities
          (dorun (map #(teams/create-team request (:_id %)) (filter #(= (:type %) k/PROJECT-TYPE) entities)))

          (if (and (zero? (count links))
                (zero? (count updates)))
            (created {type-kw entities})
            (created {type-kw  entities
                      :links   links
                      :updates updates})))

        (catch [:type :ovation.auth/unauthorized] _
          (unauthorized {:errors {:detail "Not authorized"}})))

      (bad-request {:errors {:detail (str "Entities must be of \"type\" " type-name)}}))))

(defmacro post-resources
  "POST to resources of type (e.g. \"Project\")"
  [entity-type schemas]
  (let [type-name (capitalize entity-type)
        type-path (typepath type-name)
        type-kw (keyword type-path)]
    `(POST "/" request#
       :return {~type-kw                  [~(clojure.core/symbol "ovation.schema" type-name)]
                (s/optional-key :links)   [LinkInfo]
                (s/optional-key :updates) [Entity]}
       :body [entities# {~type-kw [(apply s/either ~schemas)]}]
       :summary ~(str "Creates a new top-level " type-name)
       (post-resources* request# ~type-name ~type-kw (~type-kw entities#)))))

(defmacro get-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)
        single-type-kw (keyword (lower-case type-name))]
    `(GET "/" request#
       :name ~(keyword (str "get-" (lower-case type-name)))
       :return {~single-type-kw ~(clojure.core/symbol "ovation.schema" type-name)}
       :summary ~(str "Returns " type-name " with :id")
       (let [auth# (auth/identity request#)]
         (if-let [entities# (core/get-entities auth# [~id] (r/router request#))]
           (if-let [projects# (seq (filter #(= ~type-name (:type %)) entities#))]
             (ok {~single-type-kw (first projects#)})
             (not-found {:errors {:detail "Not found"}})))))))

(defn-traced make-child-link*
  [auth sources target-ids source-type routes]
  (fn [target]
    (let [target-type (util/entity-type-keyword target)
          rel (get-in EntityChildren [source-type target-type :rel])
          inverse-rel (get-in EntityChildren [source-type target-type :inverse-rel])]

      (if rel
        (links/add-links auth sources rel target-ids routes :inverse-rel inverse-rel)
        {}))))

(defn-traced make-child-links*
  [auth parent-id type-name targets routes]
  (let [target-ids (map :_id targets)
        sources    (core/get-entities auth [parent-id] routes)
        type       (util/entity-type-name-keyword type-name)
        results    (map (make-child-link* auth sources target-ids type routes) targets)
        links      (mapcat :links results)
        updates    (mapcat :updates results)]
    {:links links
     :updates updates}))


(defn-traced post-resource*
  [request type-name id body]
  (let [auth (auth/identity request)]
    (try+

      (let [routes           (r/router request)
            cleaned-entities (remove-embedded-relationships body)
            entities         (core/create-entities auth cleaned-entities routes :parent id)
            rel-map          (relationships-map body entities) ;; NB Assumes result of create-entities is in the same order as body!!
            child-links      (make-child-links* auth id type-name entities routes)
            embedded-links   (embedded-links auth entities rel-map routes)
            links            (core/create-values auth routes (mapcat :links (cons child-links embedded-links))) ;; Combine all :links from child and embedded
            updates          (core/update-entities auth (mapcat :updates (cons child-links embedded-links)) routes :authorize false :update-collaboration-roots true)] ;; Combine all :updates from child and embedded links

        (created {:entities entities
                  :links    links
                  :updates updates}))

      (catch [:type :ovation.auth/unauthorized] err
        (unauthorized {:errors {:detail "Not authorized to create new entities"}})))))


(defmacro post-resource
  [entity-type id schemas]
  (let [type-name (capitalize entity-type)]
    `(POST "/" request#
       :name ~(keyword (format "create-%s-entity" (lower-case type-name)))
       :return {:entities [(apply s/either ~schemas)]
                :links    [LinkInfo]
                :updates  [Entity]}
       :body [body# {:entities [(apply s/either ~schemas)]}]
       :summary ~(str "Creates and returns a new entity with the identified " type-name " as collaboration root")
       (post-resource* request# ~type-name ~id (:entities body#)))))


(defn-traced put-resource*
  [request id type-name type-kw updates]
  (let [entity-id (str (:_id updates))]
    (if-not (= id (str entity-id))
      (not-found {:error (str type-name " " entity-id " ID mismatch")})
      (try+
        (let [auth (auth/identity request)
              entity (first (core/update-entities auth [updates] (r/router request)))]
          (ok {type-kw entity}))

        (catch [:type :ovation.auth/unauthorized] _
          (unauthorized {:errors {:detail "Unauthorized"}}))
        (catch [:type :ovation.couch/conflict] err
          (conflict {:errors {:detail "Document update conflict"
                              :id     (:id err)}}))
        (catch [:type :ovation.couch/forbidden] err
          (forbidden {:errors {:detail "Document update forbidden"
                               :id     (:id err)}}))
        (catch [:type :ovation.couch/unauthorized] err
          (unauthorized {:errors {:detail "Document update unauthorized"
                                  :id     (:id err)}}))))))

(defmacro put-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)
        update-type (format "%sUpdate" type-name)
        type-kw (util/entity-type-name-keyword type-name)]
    `(PUT "/" request#
       :name ~(keyword (str "update-" (lower-case type-name)))
       :return {~type-kw ~(clojure.core/symbol "ovation.schema" type-name)}
       :body [updates# {~type-kw ~(clojure.core/symbol "ovation.schema" update-type)}]
       :summary ~(str "Updates and returns " type-name " with :id")
       (put-resource* request# ~id ~type-name ~type-kw (~type-kw updates#)))))

(defmacro delete-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)]

    `(DELETE "/" request#
       :name ~(keyword (str "delete-" (lower-case type-name)))
       :return {:entity TrashedEntity}
       :summary ~(str "Deletes (trashes) " type-name " with :id")
       (try+
         (let [auth# (auth/identity request#)]
           (accepted {:entity (first (core/delete-entities auth# [~id] (r/router request#)))}))
         (catch [:type :ovation.auth/unauthorized] err#
           (unauthorized {}))))))

(defn-traced rel-related*
  [request id rel routes]
  (let [auth (auth/identity request)
        related (links/get-link-targets auth id (lower-case rel) routes)]
   (ok {(keyword rel) related})))


(defmacro rel-related
  [entity-type id rel]
  (let [type-name (capitalize entity-type)]
    `(GET "/" request#
       :name ~(keyword (str "get-" (lower-case type-name) "-link-targets"))
       :return {s/Keyword [Entity]}
       :summary ~(str "Gets the targets of relationship :rel from the identified " type-name)
       (rel-related* request# ~id ~rel (r/router request#)))))

(defn-traced get-relationships*
  [request id rel]
  (let [auth (auth/identity request)
        rels (links/get-links auth id rel (r/router request))]
    (ok {:links rels})))


(defn-traced post-relationships*
  [request id new-links rel]
  (try+
    (let [auth (auth/identity request)
          source (first (core/get-entities auth [id] (r/router request)))
          routes (r/router request)]
      (when source
        (auth/check! auth ::auth/update source))
      (if source
        (let [groups (group-by :inverse_rel new-links)
              link-groups (map (fn [[irel nlinks]] (links/add-links auth [source] rel (map :target_id nlinks) routes :inverse-rel irel)) (seq groups))]
          (let [links (core/create-values auth routes (flatten (map :links link-groups)))
                updates (core/update-entities auth (flatten (map :updates link-groups)) routes :authorize false  :update-collaboration-roots true)]
            (created {:updates updates
                      :links   links})))
        (not-found {:errors {:detail (str ~id " not found")}})))
    (catch [:type :ovation.auth/unauthorized] {:keys [message]}
      (unauthorized {:errors {:detail message}}))
    (catch [:type :ovation.links/target-not-found] {:keys [message]}
      (bad-request {:errors {:detail message}}))))

(defmacro relationships
  [entity-type id rel]
  (let [type-name (capitalize entity-type)]
    `(context "/relationships" []
       (GET "/" request#
         :name ~(keyword (str "get-" (lower-case type-name) "-links"))
         :return {:links [LinkInfo]}
         :summary ~(str "Get relationships for :rel from " type-name " :id")
         (get-relationships* request# ~id ~rel))

       (POST "/" request#
         :name ~(keyword (str "create-" (lower-case type-name) "-links"))
         :return {:links   [LinkInfo]
                  :updates [Entity]}
         :body [new-links# [NewLink]]
         :summary ~(str "Add relationship links for :rel from " type-name " :id")
         (post-relationships* request# ~id new-links# ~rel)))))

(defn-traced post-revisions*
  [request id revisions]
  (let [auth (auth/identity request)]
    (try+
      (let [routes                   (r/router request)
            parent                   (core/get-entity auth id routes)
            revisions-with-ids       (map #(assoc % :_id (str (util/make-uuid))) revisions)
            revisions-with-resources (revisions/make-resources auth revisions-with-ids)
            result                   (revisions/create-revisions auth routes parent (map :revision revisions-with-resources))
            links                    (future (core/create-values auth routes (:links result)))
            updates                  (future (core/update-entities auth (:updates result) routes :update-collaboration-roots true))]

        {:entities (:revisions result)
         :links     @links
         :updates   @updates
         :aws       (map (fn [m] {:id  (get-in m [:revision :_id])
                                  :aws (walk/keywordize-keys (:aws m))}) revisions-with-resources)})
      (catch [:type :ovation.revisions/file-revision-conflict] err
        (conflict {:errors {:detail (:message err)}})))))

(defn-traced get-head-revisions*
  [request id]
  (let [auth   (auth/identity request)
        routes (r/router request)]

    (try+
      (ok {:revisions (revisions/get-head-revisions auth routes id)})
      (catch [:type ::revisions/not-found] _
        (not-found! {:errors {:detail "File not found"}})))))

(defn- rel
  [src dest]
  (get-in EntityChildren [(util/entity-type-keyword src) (util/entity-type-keyword dest) :rel]))

(defn inverse-rel
  [src dest]
  (get-in EntityChildren [(util/entity-type-keyword src) (util/entity-type-keyword dest) :inverse-rel]))

(defn-traced move-contents*
  [request id info]
  (let [routes (r/router request)
        auth   (auth/identity request)

        src    (core/get-entities auth [(:source info)] routes)
        dest   (core/get-entities auth [(:destination info)] routes)
        entity (first (core/get-entities auth [id] routes))]

    (if (and
          (contains? #{k/FILE-TYPE k/FOLDER-TYPE} (:type entity))
          (contains? #{k/FOLDER-TYPE k/PROJECT-TYPE} (:type (first src)))
          (contains? #{k/FOLDER-TYPE k/PROJECT-TYPE} (:type (first dest))))

      (let [added (links/add-links auth dest (rel (first dest) entity) [id] routes :inverse-rel (inverse-rel (first dest) entity))
            links (future (core/create-values auth routes (:links added)))
            updates (future (core/update-entities auth (:updates added) routes :authorize false :update-collaboration-roots true))]

        (do
          (links/delete-links auth routes (first src) (rel (first src) entity) id)

          {(util/entity-type-keyword entity)    entity
           :updates @updates
           :links   @links}))

      (unprocessable-entity! {:errors {:detail "Unexpected entity type"}}))))
