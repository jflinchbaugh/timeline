(ns timeline.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [timeline.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[timeline started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[timeline has shut down successfully]=-"))
   :middleware wrap-dev})
