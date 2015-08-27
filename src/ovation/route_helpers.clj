(ns ovation.route-helpers
  (:require [compojure.api.sweet :refer :all]
            [ovation.annotations :as annotations]
            [schema.core :as s]
            [ring.util.http-response :refer [created ok accepted not-found unauthorized bad-request]]
            [ovation.core :as core]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure.string :refer [lower-case capitalize join]]
            [ovation.schema :refer :all]))

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
             entities# (core/of-type auth# ~type-name)]
         (ok {~type-kw entities#})))))

(defmacro post-resources
  "POST to resources of type (e.g. \"Project\")"
  [entity-type]
  (let [type-name (capitalize entity-type)
        type-path (lower-case (str type-name "s"))
        type-kw (keyword type-path)]
    `(POST* "/" request#
       :name ~(keyword (str "create-" (lower-case type-name)))
       :return {~type-kw [Entity]}
       :body [entities# [NewEntity]]
       :summary ~(str "Creates a new top-level " type-name)
       (let [auth# (:auth/auth-info request#)]
         (if (every? #(= ~type-name (:type %)) entities#)
           (try+
             (created {~type-kw (core/create-entity auth# entities#)})

             (catch [:type :ovation.auth/unauthorized] err#
               (unauthorized {})))

           (bad-request (str "Entities must be of \"type\" " ~type-name)))))))

(defmacro get-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)
        single-type-kw (keyword (lower-case type-name))]
    `(GET* "/" request#
       :name ~(keyword (str "get-" (lower-case type-name)))
       :return {~single-type-kw Entity}
       :summary ~(str "Returns " type-name " with :id")
       (let [auth# (:auth/auth-info request#)]
         (if-let [entities# (core/get-entities auth# [~id])]
           (if-let [projects# (seq (filter #(= ~type-name (:type %)) entities#))]
             (ok {~single-type-kw (first projects#)})
             (not-found {})))))))

(defmacro post-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)]
    `(POST* "/" request#
       :name ~(keyword (str "create-" (lower-case type-name) "-entity"))
       :return {:entities [Entity]}
       :body [entities# [NewEntity]]
       :summary ~(str "Creates and returns a new entity with the identified " type-name " as collaboration root")
       (let [auth# (:auth/auth-info request#)]
         (try+
           (created {:entities (core/create-entity auth# entities# :parent ~id)})

           (catch [:type :ovation.auth/unauthorized] err#
             (unauthorized {})))))))

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
                   entities# (core/update-entity auth# [update#])]
               (ok {~type-kw entities#}))

             (catch [:type :ovation.auth/unauthorized] err#
               (unauthorized {}))))))))

(defmacro delete-resource
  [entity-type id]
  (let [type-name (capitalize entity-type)]

    `(DELETE* "/" request#
       :name ~(keyword (str "delete-" (lower-case type-name)))
       :return {:entities [TrashedEntity]}
       :summary ~(str "Deletes (trashes) " type-name " with :id")
       (try+
         (let [auth# (:auth/auth-info request#)]
           (accepted {:entities (core/delete-entity auth# [~id])}))
         (catch [:type :ovation.auth/unauthorized] err#
           (unauthorized {}))))))
