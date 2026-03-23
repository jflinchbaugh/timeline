(ns com.hjsoft.timeline.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [ajax.core :refer [GET json-response-format]]
            ["react-dom/client" :as rdom]
            [com.hjsoft.timeline.logic :as logic]
            [com.hjsoft.timeline.ui :as ui]))

(defn- update-theme [theme]
  (let [root (.-documentElement js/document)]
    (.setProperty (.-style root) "--primary" (:primaryColor theme))
    (.setProperty (.-style root) "--bg" (:backgroundColor theme))
    (.setProperty (.-style root) "--text" (:textColor theme))
    (.setProperty (.-style root) "--card-bg" (:cardColor theme))))

(defn- update-url-param [key value]
  (let [url (js/URL. js/window.location.href)]
    (.set (.-searchParams url) key value)
    (.pushState js/window.history #js {} "" (.toString url))))

(defnc app []
  (let [[current-file set-current-file]
        (hooks/use-state
         (or
          (.get (js/URLSearchParams. js/window.location.search) "data")
          "history.json"))
        [data set-data] (hooks/use-state nil)
        [game set-game] (hooks/use-state nil)
        [loading? set-loading] (hooks/use-state true)]

    (hooks/use-effect [current-file]
      (let [url (str "data/" current-file)
            on-success (fn [res]
                         (set-data (js->clj res :keywordize-keys true))
                         (set-loading false))
            on-error (fn [e]
                       (js/console.error "Failed to load data" e)
                       (set-loading false))]
        (set-loading true)
        (GET url
          {:handler on-success
           :response-format (json-response-format {:keywords? true})
           :error-handler on-error})))

    (hooks/use-effect [data]
      (when data
        (update-theme (:theme data))))

    (let [handle-select-data (fn [file]
                               (set-current-file file)
                               (update-url-param "data" file))
          handle-start (fn [names]
                         (set-game (logic/init-game (:events data) names)))
          handle-action (fn [action & args]
                          (case action
                            :place (let [idx (first args)]
                                     (set-game #(logic/place-card % idx)))
                            :next-turn (set-game logic/next-turn)
                            :restart (set-game nil)))]
      (if loading?
        (d/div "Loading game data...")
        (if-not game
          ($ ui/setup-screen
             {:current-file current-file
              :on-select-data handle-select-data
              :on-start handle-start})
          ($ ui/game-screen
             {:game game
              :on-action handle-action}))))))

;; --- Entry Point ---

(defonce root-atom (atom nil))

(defn init! []
  (let [el (js/document.getElementById "app")]
    (when (and el (not @root-atom))
      (reset! root-atom (rdom/createRoot el))))
  (when @root-atom
    (.render ^js @root-atom ($ app))))

(defn ^:before-load stop! []
  (when @root-atom
    (.unmount ^js @root-atom)
    (reset! root-atom nil)))

(defn ^:after-load reload! []
  (init!))
