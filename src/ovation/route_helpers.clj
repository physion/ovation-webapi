(ns ovation.route-helpers
  (:require [compojure.api.sweet :refer :all]
            [ovation.annotations :as annotations]
            [schema.core :as s]
            [ring.util.http-response :refer [created ok accepted not-found unauthorized bad-request]]
            [ovation.core :as core]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.string :refer [lower-case capitalize upper-case join]]
            [ovation.schema :refer :all]
            [ovation.links :as links]
            [ovation.util :as util]
            [ovation.routes :as r]))

(defmacro annotation
  "Creates an annotation type endpoint"
  [id annotation-description annotation-key record-schema annotation-schema]

  `(context* ~(str "/" annotation-key) []
     :tags [~annotation-key]
     (GET* "/" request#
       :name ~(keyword (str "get-" (lower-case annotation-key)))
       ;:return {s/Keyword [~annotation-schema]}
       :summary ~(str "Returns all " annotation-description " annotations associated with entity :id")
       (let [auth# (:auth/auth-info request#)
             annotations# (annotations/get-annotations auth# [~id] ~annotation-key)]
         (ok {(keyword ~annotation-key) annotations#})))

     (POST* "/" request#
       :name ~(keyword (str "create-" (lower-case annotation-key)))
       :return {s/Keyword [~annotation-schema]}
       :body [new-annotations# [~record-schema]]
       :summary ~(str "Adds a new " annotation-description " annotation to entity :id")
       (let [auth# (:auth/auth-info request#)]
         (created {(keyword ~annotation-key) (annotations/create-annotations auth# [~id] ~annotation-key new-annotations#)})))

     (context* "/:annotation-id" [aid#]
       (DELETE* "/" request#
         :name ~(keyword (str "delete-" (lower-case annotation-key)))
         :return [s/Str]
         :summary ~(str "Removes a " annotation-description " annotation from entity :id")
         (let [auth# (:auth/auth-info request#)
               annotation-id# (-> request# :route-params :annotation-id)]
           (accepted (map :_id (annotations/delete-annotations auth# [annotation-id#]))))))))


(defmacro get-resources
  "Get all resources of type (e.g. \"Project\")"
  [entity-type]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(GET* "/" request#
       :name ~(keyword (str "all-" (lower-case type-name)))
       :return {~type-kw [Entity]}
       :summary (str "Gets all top-level " ~type-path)
       (let [auth# (:auth/auth-info request#)
             entities# (core/of-type auth# ~type-name (r/router request#))]
         (ok {~type-kw entities#})))))

(defmacro post-resources
  "POST to resources of type (e.g. \"Project\")"
  [entity-type schemas]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(POST* "/" request#
       :return {~type-kw [Entity]}
       :body [entities# ~schemas]
       :summary ~(str "Creates a new top-level " type-name)
       (let [auth# (:auth/auth-info request#)]
         (if (every? #(= ~type-name (:type %)) entities#)
           (try+
             (created {~type-kw (core/create-entity auth# entities# (r/router request#))})

             (catch [:type :ovation.auth/unauthorized] err#
               (unauthorized {:errors {:detail "Not authorized"}})))

           (bad-request {:errors {:detail (str "Entities must be of \"type\" " ~type-name)}}))))))

(defmacro get-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)
        single-type-kw (keyword (lower-case type-name))]
    `(GET* "/" request#
       :name ~(keyword (str "get-" (lower-case type-name)))
       :return {~single-type-kw Entity}
       :summary ~(str "Returns " type-name " with :id")
       (let [auth# (:auth/auth-info request#)]
         (if-let [entities# (core/get-entities auth# [~id] (r/router request#))]
           (if-let [projects# (seq (filter #(= ~type-name (:type %)) entities#))]
             (ok {~single-type-kw (first projects#)})
             (not-found {:errors {:detail "Not found"}})))))))

(defmacro post-resource
  [entity-type id schemas]
  (let [type-name (capitalize entity-type)]
    `(POST* "/" request#
       :name ~(keyword (str "create-" (lower-case type-name) "-entity"))
       :return {:entities [Entity]
                          :links [LinkInfo]}
       :body [entities# ~schemas]
       :summary ~(str "Creates and returns a new entity with the identified " type-name " as collaboration root")
       (let [auth# (:auth/auth-info request#)]
         (try+
           (let [
                 routes# (r/router request#)
                 self# (core/get-entities auth# ~id routes#)
                 entities# (core/create-entity auth# entities# routes# :parent ~id)
                 entity_ids# (map :_id entities#)

                 links# (map (fn [target#]
                               (let [rel# (get-in EntityChildren [~(util/entity-type-name-keyword type-name) (keyword (clojure.string/lower-case (:type target#))) :rel])
                                     inverse_rel# (get-in EntityChildren [~(util/entity-type-name-keyword type-name) (keyword (clojure.string/lower-case (:type target#))) :inverse_rel])]
                                 (links/add-links auth# self# [target#] rel# entity_ids# :inverse_rel inverse_rel#))) entities#)
                 ]

             (created {:entities entities#
                       :links    (apply concat (map :links links#))}))

           (catch [:type :ovation.auth/unauthorized] err#
             (unauthorized {:errors {:detail "Not authorized to create new entities"}})))))))

(defmacro put-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(PUT* "/" request#
       :name ~(keyword (str "update-" (lower-case type-name)))
       :return {~type-kw [Entity]}
       :body [update# EntityUpdate]
       :summary ~(str "Updates and returns " type-name " with :id")
       (let [entity-id# (str (:_id update#))]
         (if-not (= ~id (str entity-id#))
           (not-found {:error (str ~type-name " " entity-id# " ID mismatch")})
           (try+
             (let [auth# (:auth/auth-info request#)
                   entities# (core/update-entity auth# [update#] (r/router request#))]
               (ok {~type-kw entities#}))

             (catch [:type :ovation.auth/unauthorized] err#
               (unauthorized {:errors {:detail "Unauthorized"}}))))))))

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


(defmacro rel-related
  [entity-type id rel]
  (let [type-name (capitalize entity-type)
        rel-name (lower-case rel)]
    `(GET* "/" request#
       :name ~(keyword (str "get-" (lower-case type-name) "-link-targets"))
       :return {s/Keyword [Entity]}
       :summary ~(str "Gets the targets of relationship :rel from the identified " type-name)
       (let [auth# (:auth/auth-info request#)]
         (ok {(keyword ~rel-name) (links/get-link-targets auth# ~id ~rel-name)})))))

(defmacro relationships
  [entity-type id rel]
  (let [type-name (capitalize entity-type)]
    `(context* "/relationships" []
       (GET* "/" request#
         :name ~(keyword (str "get-" (lower-case type-name) "-links"))
         :return {:links [LinkInfo]}
         :summary ~(str "Get relationships for :rel from " type-name " :id")
         (ok {:links []}))

       (POST* "/" request#
         :name ~(keyword (str "create-" (lower-case type-name) "-links"))
         :return {:links [LinkInfo]}
         :body [new-links# [NewLink]]
         :summary ~(str "Add relationship links for :rel from " type-name " :id")
         (try+
           (let [auth# (:auth/auth-info request#)
                 source# (first (core/get-entities auth# [~id] (r/router request#)))]
             (if source#
               (let [all-updates# (:all (links/add-links auth# source# ~rel (map :target_id new-links#) (r/router request#)))
                     updates# (core/update-entity auth# all-updates# :direct true)] ;;TODO this should not use update-entity for linkinfo
                 (created {:entities (filter (fn [doc#] (not= util/RELATION_TYPE (:type doc#))) updates#)
                           :links    (filter :rel updates#)}))
               (not-found {:errors {:detail (str ~id " not found")}})))
           (catch [:type :ovation.auth/unauthorized] {:keys [message#]}
             (unauthorized {:errors {:detail message#}}))
           (catch [:type :ovation.links/target-not-found] {:keys [message#]}
             (bad-request {:errors {:detail message#}}))))
       (DELETE* "/" request#
         :name ~(keyword (str "delete-" (lower-case type-name) "-links"))
         ;; Return Accepted, or {:errors} on error
         :summary ~(str "Remove relationships for :rel from " type-name " :id")
         (accepted)                                         ;;TODO — this needs to be implemented
         ))))
