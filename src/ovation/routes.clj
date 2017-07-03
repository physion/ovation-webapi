(ns ovation.routes
  (:require [ovation.util :as util]))

(defn relationship-route
  [ctx doc name]
  (let [type (util/entity-type-name doc)
        id (:_id doc)
        rt (:ovation.request-context/routes ctx)
        org (:ovation.request-context/org ctx)
        route-name (keyword (format "get-%s-links" type))]
    (rt route-name {:org org :id id :rel name})))

(defn targets-route
  ([ctx doc name]
   (let [type (util/entity-type-name doc)
         id   (:_id doc)
         rt   (:ovation.request-context/routes ctx)
         org  (:ovation.request-context/org ctx)]
     (rt (keyword (format "get-%s-link-targets" type)) {:org org :id id :rel name})))
  ([ctx type id name]
   (let [rt  (:ovation.request-context/routes ctx)
         org (:ovation.request-context/org ctx)]
     (rt (keyword (format "get-%s-link-targets" type)) {:org org :id id :rel name}))))


(defn org-projects-route
  [rt org-id]
  (rt :all-projects {:org org-id}))

(defn org-memberships-route
  [rt org-id]
  (rt :get-org-memberships {:org org-id}))

(defn org-groups-route
  [rt org-id]
  (rt :get-org-groups {:org org-id}))

(defn group-memberships-route
  [rt org-id group-id]
  (rt :get-groups-memberships {:org org-id :id group-id}))


(defn self-route
  ([ctx doc]
   (let [type (util/entity-type-name doc)
         id   (:_id doc)
         rt   (:ovation.request-context/routes ctx)
         org  (:ovation.request-context/org ctx)]
     (rt (keyword (format "get-%s" type)) {:org org :id id})))
  ([ctx type-name id]
   (let [rt  (:ovation.request-context/routes ctx)
         org (:ovation.request-context/org ctx)]
     (rt (keyword (format "get-%s" type-name)) {:org org :id id})))
  ([ctx type-name org id]
   (let [rt  (:ovation.request-context/routes ctx)]
     (rt (keyword (format "get-%s" type-name)) {:org org :id id}))))

(defn entity-route
  [ctx id]
  (let [rt  (:ovation.request-context/routes ctx)
        org (:ovation.request-context/org ctx)]
    (rt :get-entity {:org org :id id})))

(defn named-route
  [ctx name args]
  (let [rt  (:ovation.request-context/routes ctx)]
    (rt (keyword name) args)))

(defn heads-route
  [ctx doc]
  (let [{rt  :ovation.request-context/routes
         org :ovation.request-context/org} ctx]
    (rt :file-head-revisions {:org org :id (:_id doc)})))


(defn heads-route2
  [ctx id]
  (let [{rt  :ovation.request-context/routes
         org :ovation.request-context/org} ctx]
    (rt :file-head-revisions {:org org :id id})))

(defn zip-activity-route
  [ctx doc]
  (let [{rt  :ovation.request-context/routes
         org :ovation.request-context/org} ctx]
    (rt :zip-activity {:org org :id (:_id doc)})))

(defn zip-folder-route
  [ctx doc]
  (let [{rt  :ovation.request-context/routes
         org :ovation.request-context/org} ctx]
    (rt :zip-folder {:org org :id (:_id doc)})))

(defn upload-complete-route
  [ctx doc]
  (let [{rt  :ovation.request-context/routes
         org :ovation.request-context/org} ctx]
    (rt :upload-complete {:org org :id (:_id doc)})))

(defn upload-failed-route
  [ctx doc]
  (let [{rt  :ovation.request-context/routes
         org :ovation.request-context/org} ctx]
    (rt :upload-failed {:org org :id (:_id doc)})))

(defn annotations-route
  [ctx doc annotation-type]
  (let [{rt  :ovation.request-context/routes
         org :ovation.request-context/org} ctx]
    (rt (keyword (format "get-%s" annotation-type)) {:org org :id (:_id doc)})))
