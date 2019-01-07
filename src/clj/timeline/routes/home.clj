(ns timeline.routes.home
  (:require [timeline.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [timeline.db.core :as db]))

(def events
  [
    {:date [1977 3 21] :description "Birth"}
    {:date [1981] :description "Start Preschool at Little People"}
    {:date [1982] :description "Start Kindergarten at Kreutz Creek"}
    {:date [1995 6] :description "Graduate from Eastern York High School"}
    {:date [2000 5] :description "Graduate from Millersville University"}
    {:date [2000 6] :description "Start at Educators Mutual Life Insurance"}
    {:date [2002 8 10] :description "Married"}
    {:date [2002 12] :description "Moved to Armstrong Ln, Lancaster"}
    {:date [2003 5 10] :description "Paige Born"}
    {:date [2005 7 13] :description "Benedict Born"}
    {:date [2008 1] :description "Started Using Google Calendar"}
    {:date [2008 4 28] :description "Hosted First Open Space at MapQuest"}
    {:date [2008 11 28] :description "Attend Photo Class at HACC"}
    {:date [2009 4 26] :description "First Senior Portraits with Bridget"}
    {:date [2009 4] :description "Start Using a Standing Desk"}
    {:date [2010 12 16] :description "Purchase 2011 Ford Fiesta"}
    {:date [2012 5 22] :description "Start at Learning Sciences International"}
    {:date [2014 10 15] :description "Left Learning Science International"}
    {:date [2014 11 10] :description "Start at Hershey Medical Center"}
    {:date [2014 11 24] :description "Separated"}
    {:date [2015 12 31] :description "Divorce"}
    {:date [2017 10 21] :description "Purchase 2014 BMW i3"}
    {:date [2018 12 06] :description "Purchase House"}
    {:date [2019 01 10] :description "Leave Hershey Medical Center"}
  ]
)

(map :date events)
(map :description events)

(def date {:date [2012 10]})

(def omitted not)

(defn dayOfMonth [event]
  (let [
    [_ _ day] (:date event)]
    day))

(defn monthOfYear [event]
  (let [
    [_ month _] (:date event)]
    month))

(defn year [event]
  (let [
    [year _ _] (:date event)]
    year))

(dayOfMonth {:date [1 2 3]})

(comment all ways to get events with dayOfMonth)
(filter (fn [d] (-> d dayOfMonth)) events)
(filter (fn [d] (dayOfMonth d)) events)
(filter #(-> %1 dayOfMonth) events)
(filter #(dayOfMonth %1) events)
(filter dayOfMonth events)

(comment all ways to get events without dayOfMonth)
(filter (fn [d] (-> d dayOfMonth not)) events)
(filter #(-> %1 dayOfMonth not) events)
(filter #(-> %1 dayOfMonth omitted) events)
(filter (fn [d] (not (dayOfMonth d))) events)
(filter #(not (dayOfMonth %1)) events)
(filter (complement dayOfMonth) events)
(filter (comp not dayOfMonth) events)

(map year (filter (complement dayOfMonth) events))

(defn home-page []
  (layout/render
    "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn about-page []
  (layout/render "about.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page)))

