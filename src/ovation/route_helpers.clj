(ns ovation.route-helpers
  (:require [compojure.api.sweet :refer :all]
            [ovation.annotations :as annotations]
            [schema.core :as s]
            [ring.util.http-response :refer [created ok accepted not-found not-found! unauthorized bad-request bad-request! conflict forbidden]]
            [ovation.core :as core]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.string :refer [lower-case capitalize upper-case join]]
            [ovation.schema :refer :all]
            [ovation.links :as links]
            [ovation.util :as util]
            [ovation.routes :as r]
            [ovation.auth :as auth]
            [ovation.revisions :as revisions]
            [clojure.walk :as walk]))

(defn get-annotations*
  [request id annotation-key]
  (let [auth (:auth/auth-info request)
        annotations (annotations/get-annotations auth [id] annotation-key)]
    (ok {(keyword annotation-key) annotations})))

(defn post-annotations*
  [request id annotation-key annotations]
  (let [auth (:auth/auth-info request)
        annotations-kw (keyword annotation-key)]
    (created {annotations-kw (annotations/create-annotations auth (r/router request) [id] annotation-key annotations)})))

(defn delete-annotations*
  [request annotation-key]
  (let [auth (:auth/auth-info request)
        annotation-id (-> request :route-params :annotation-id)]
    (accepted {(keyword annotation-key) (annotations/delete-annotations auth [annotation-id] (r/router request))})))

(defmacro annotation
  "Creates an annotation type endpoint"
  [id annotation-description annotation-key record-schema annotation-schema]

  (let [annotation-kw (keyword annotation-key)]
    `(context* ~(str "/" annotation-key) []
       :tags [~annotation-key]
       (GET* "/" request#
         :name ~(keyword (str "get-" (lower-case annotation-key)))
         :return {~annotation-kw [~annotation-schema]}
         :summary ~(str "Returns all " annotation-description " annotations associated with entity :id")
         (get-annotations* request# ~id ~annotation-key))

       (POST* "/" request#
         :name ~(keyword (str "create-" (lower-case annotation-key)))
         :return {(keyword ~annotation-key) [~annotation-schema]}
         :body [new-annotations# {(keyword ~annotation-key) [~record-schema]}]
         :summary ~(str "Adds a new " annotation-description " annotation to entity :id")
         (post-annotations* request# ~id ~annotation-key ((keyword ~annotation-key) new-annotations#)))

       (context* "/:annotation-id" [aid#]
         (DELETE* "/" request#
           :name ~(keyword (str "delete-" (lower-case annotation-key)))
           :return [s/Str]
           :summary ~(str "Removes a " annotation-description " annotation from entity :id. Returns the deleted annotations.")
           (delete-annotations* request# ~annotation-key))))))


(defmacro get-resources
  "Get all resources of type (e.g. \"Project\")"
  [entity-type]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(GET* "/" request#
       :name ~(keyword (str "all-" (lower-case type-name)))
       :return {~type-kw [~(clojure.core/symbol "ovation.schema" type-name)]}
       :summary (str "Gets all top-level " ~type-path)
       (let [auth# (:auth/auth-info request#)
             entities# (core/of-type auth# ~type-name (r/router request#))]
         (ok {~type-kw entities#})))))

(defn post-resources*
  [request type-name type-kw entities]
  (let [auth (:auth/auth-info request)]
    (if (every? #(= type-name (:type %)) entities)
      (try+
        (created {type-kw (core/create-entities auth entities (r/router request))})

        (catch [:type :ovation.auth/unauthorized] err
          (unauthorized {:errors {:detail "Not authorized"}})))

      (bad-request {:errors {:detail (str "Entities must be of \"type\" " type-name)}}))))

(defmacro post-resources
  "POST to resources of type (e.g. \"Project\")"
  [entity-type schemas]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(POST* "/" request#
       :return {~type-kw [~(clojure.core/symbol "ovation.schema" type-name)]}
       :body [entities# {~type-kw [(apply s/either ~schemas)]}]
       :summary ~(str "Creates a new top-level " type-name)
       (post-resources* request# ~type-name ~type-kw (~type-kw entities#)))))

(defmacro get-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)
        single-type-kw (keyword (lower-case type-name))]
    `(GET* "/" request#
       :name ~(keyword (str "get-" (lower-case type-name)))
       :return {~single-type-kw ~(clojure.core/symbol "ovation.schema" type-name)}
       :summary ~(str "Returns " type-name " with :id")
       (let [auth# (:auth/auth-info request#)]
         (if-let [entities# (core/get-entities auth# [~id] (r/router request#))]
           (if-let [projects# (seq (filter #(= ~type-name (:type %)) entities#))]
             (ok {~single-type-kw (first projects#)})
             (not-found {:errors {:detail "Not found"}})))))))

(defn make-child-link*
  [auth sources target-ids source-type routes]
  (fn [target]
    (let [target-type (util/entity-type-keyword target)
          rel (get-in EntityChildren [source-type target-type :rel])
          inverse-rel (get-in EntityChildren [source-type target-type :inverse-rel])]

      (if rel
        (links/add-links auth sources rel target-ids routes :inverse-rel inverse-rel)
        {}))))

(defn make-child-links*
  [auth parent-id type-name targets routes]
  (let [target-ids (map :_id targets)
        sources (core/get-entities auth [parent-id] routes)
        type (util/entity-type-name-keyword type-name)
        results (map (make-child-link* auth sources target-ids type routes) targets)
        links (apply concat (map :links results))
        updates (apply concat (map :updates results))]
    {:links links
     :updates updates}))

(defn post-resource*
  [request type-name id body]
  (let [auth (:auth/auth-info request)]
    (try+
      (let [
            routes (r/router request)
            entities (core/create-entities auth body routes :parent id)
            child-links (make-child-links* auth id type-name entities routes)
            links (core/create-values auth routes (:links child-links))
            updates (core/update-entities auth (:updates child-links) routes)]

        (created {:entities entities
                  :links    links
                  :updates updates}))

      (catch [:type :ovation.auth/unauthorized] err
        (unauthorized {:errors {:detail "Not authorized to create new entities"}}))))
  )

(defmacro post-resource
  [entity-type id schemas]
  (let [type-name (capitalize entity-type)]
    `(POST* "/" request#
       :name ~(keyword (format "create-%s-entity" (lower-case type-name)))
       :return {:entities [(apply s/either ~schemas)]
                :links    [LinkInfo]
                :updates  [Entity]}
       :body [body# {:entities [(apply s/either ~schemas)]}]
       :summary ~(str "Creates and returns a new entity with the identified " type-name " as collaboration root")
       (post-resource* request# ~type-name ~id (:entities body#)))))


(defn put-resource*
  [request id type-name type-kw updates]
  (let [entity-id (str (:_id updates))]
    (if-not (= id (str entity-id))
      (not-found {:error (str type-name " " entity-id " ID mismatch")})
      (try+
        (let [auth (:auth/auth-info request)
              entity (first (core/update-entities auth [updates] (r/router request)))]
          (ok {type-kw entity}))

        (catch [:type :ovation.auth/unauthorized] err
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
        type-path (lower-case (str type-name "s"))
        type-kw (util/entity-type-name-keyword type-name)]
    `(PUT* "/" request#
       :name ~(keyword (str "update-" (lower-case type-name)))
       :return {~type-kw ~(clojure.core/symbol "ovation.schema" type-name)}
       :body [updates# {~type-kw ~(clojure.core/symbol "ovation.schema" update-type)}]
       :summary ~(str "Updates and returns " type-name " with :id")
       (put-resource* request# ~id ~type-name ~type-kw (~type-kw updates#)))))

(defmacro delete-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)]

    `(DELETE* "/" request#
       :name ~(keyword (str "delete-" (lower-case type-name)))
       :return {:entities [TrashedEntity]}
       :summary ~(str "Deletes (trashes) " type-name " with :id")
       (try+
         (let [auth# (:auth/auth-info request#)]
           (accepted {:entities (core/delete-entity auth# [~id] (r/router request#))}))
         (catch [:type :ovation.auth/unauthorized] err#
           (unauthorized {}))))))

(defn rel-related*
  [request id rel routes]
  (let [auth (:auth/auth-info request)
        related (links/get-link-targets auth id (lower-case rel) routes)]
  (ok {(keyword rel) related})))


(defmacro rel-related
  [entity-type id rel]
  (let [type-name (capitalize entity-type)]
    `(GET* "/" request#
       :name ~(keyword (str "get-" (lower-case type-name) "-link-targets"))
       :return {s/Keyword [Entity]}
       :summary ~(str "Gets the targets of relationship :rel from the identified " type-name)
       (rel-related* request# ~id ~rel (r/router request#)))))

(defn get-relationships*
  [request id rel]
  (let [auth (:auth/auth-info request)
        rels (links/get-links auth id rel (r/router request))]
    (ok {:links rels})))


(defn post-relationships*
  [request id new-links rel]
  (try+
    (let [auth (:auth/auth-info request)
          source (first (core/get-entities auth [id] (r/router request)))
          routes (r/router request)]
      (when source
        (auth/check! auth :auth/update source))
      (if source
        (let [groups (group-by :inverse_rel new-links)
              link-groups (map (fn [[irel nlinks]] (links/add-links auth [source] rel (map :target_id nlinks) routes :inverse-rel irel)) (seq groups))]
          (let [links (core/create-values auth routes (flatten (map :links link-groups)))
                updates (core/update-entities auth (flatten (map :updates link-groups)) routes)]
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
    `(context* "/relationships" []
       (GET* "/" request#
         :name ~(keyword (str "get-" (lower-case type-name) "-links"))
         :return {:links [LinkInfo]}
         :summary ~(str "Get relationships for :rel from " type-name " :id")
         (get-relationships* request# ~id ~rel))

       (POST* "/" request#
         :name ~(keyword (str "create-" (lower-case type-name) "-links"))
         :return {:links   [LinkInfo]
                  :updates [Entity]}
         :body [new-links# [NewLink]]
         :summary ~(str "Add relationship links for :rel from " type-name " :id")
         (post-relationships* request# ~id new-links# ~rel)))))

(defn post-revisions*
  [request id revisions]
  (let [auth (:auth/auth-info request)]
    (try+
      (let [routes (r/router request)
            parent (first (core/get-entities auth [id] routes))
            revisions-with-ids (map #(assoc % :_id (str (util/make-uuid))) revisions)
            revisions-with-resources (revisions/make-resources auth revisions-with-ids)
            result (revisions/create-revisions auth routes parent (map :revision revisions-with-resources))
            links (core/create-values auth routes (:links result))
            updates (core/update-entities auth (:updates result) routes)]

        {:entities (:revisions result)
         :links     links
         :updates   updates
         :aws       (map (fn [m] {:id  (get-in m [:revision :_id])
                                  :aws (walk/keywordize-keys (:aws m))}) revisions-with-resources)})
      (catch [:type :ovation.revisions/file-revision-conflict] err
        (conflict {:errors {:detail (:message err)}})))))

(defn get-head-revisions*
  [request id]
  (let [auth (:auth/auth-info request)
        routes (r/router request)
        file (first (core/get-entities auth [id] routes))]

    (when (nil? file)
      (not-found! {:errors {:detail "File not found"}}))

    ;(when-not (= "File" (:type file))
    ;  (bad-request! {:errors {:detail "Entity is not a File"}}))

    (ok {:revisions (revisions/get-head-revisions auth routes file)})))
