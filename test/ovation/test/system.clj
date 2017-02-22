(ns ovation.test.system
  (:require [com.stuartsierra.component :as component]
            [ovation.config :as config]
            [ovation.system :as system]))

(def system-config
  {:web {:port 3000}
   :db  {:host     (config/config :cloudant-db-url)
         :username (config/config :cloudant-username)
         :password (config/config :cloudant-password)}})

(def test-system nil)


(defn init []
  (alter-var-root #'test-system
    (constantly (system/create-system system-config))))

(defn start []
  (alter-var-root #'test-system component/start))

(defn stop []
  (alter-var-root #'test-system
    #(when % (component/stop %))))

(defn go []
  (init)
  (start))

(defn down? [test-system]
  (let [c (::count (meta test-system))]
    (or (nil? c) (zero? c))))

(defn update-meta [m k f]
  (with-meta m (update-in (meta m) [k] f)))

(defn system-up! [test-system]
  (alter-var-root
    test-system
    (fn [system]
      (-> system
        (if (down? system) (go))
        (update-meta ::count (fnil inc 0))))))

(defn system-down! [test-system]
  (alter-var-root
    test-system
    (fn [system]
      (if (down? system)
        system
        (-> system
          (if (= 1 (::count (meta system))) (stop))
          (update-meta ::count dec))))))

(defmacro system-background [ & body]
  `(try
     (go)
     (do ~@body)
     (finally
       (stop))))

(defn get-app []
  (get-in test-system [:api :handler]))
