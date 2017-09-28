(ns user
  (:require [mount.core :as mount]
            timeline.core))

(defn start []
  (mount/start-without #'timeline.core/repl-server))

(defn stop []
  (mount/stop-except #'timeline.core/repl-server))

(defn restart []
  (stop)
  (start))


