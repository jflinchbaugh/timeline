(ns com.hjsoft.timeline.core
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [ajax.core :refer [GET json-response-format]]
            ["react-dom/client" :as rdom]
            [com.hjsoft.timeline.logic :as logic]
            [com.hjsoft.timeline.ui :as ui]))

(defnc App []
  (let [[current-file set-current-file]
        (hooks/use-state
         (or
          (.get (js/URLSearchParams. js/window.location.search) "data")
          "history.json"))
        [data set-data] (hooks/use-state nil)
        [game set-game] (hooks/use-state nil)
        [loading? set-loading] (hooks/use-state true)]

    (hooks/use-effect [current-file]
                      (let [url (str "data/" current-file)]
                        (set-loading true)
                        (GET url
                          {:handler (fn [res]
                                      (set-data
                                       (js->clj res :keywordize-keys true))
                                      (set-loading false))
                           :response-format (json-response-format
                                             {:keywords? true})
                           :error-handler (fn [e]
                                            (js/console.error
                                             "Failed to load data"
                                             e)
                                            (set-loading false))})))

    (hooks/use-effect [data]
                      (when data
                        (let [theme (:theme data)
                              root (.-documentElement js/document)]
                          (.setProperty
                           (.-style root)
                           "--primary"
                           (:primaryColor theme))
                          (.setProperty
                           (.-style root)
                           "--bg"
                           (:backgroundColor theme))
                          (.setProperty
                           (.-style root)
                           "--text"
                           (:textColor theme))
                          (.setProperty
                           (.-style root)
                           "--card-bg"
                           (:cardColor theme)))))

    (if loading?
      (d/div "Loading game data...")
      (if-not game
        ($ ui/SetupScreen
           {:current-file current-file
            :on-select-data (fn [file]
                              (set-current-file file)
                              (let [url (js/URL. js/window.location.href)]
                                (.set (.-searchParams url) "data" file)
                                (.pushState
                                 js/window.history
                                 #js {}
                                 ""
                                 (.toString url))))
            :on-start (fn [names]
                        (set-game (logic/init-game (:events data) names)))})
        ($ ui/GameScreen
           {:game game
            :on-action (fn [action & args]
                         (case action
                           :place
                           (let [idx (first args)]
                             (set-game
                              (fn [g]
                                (let [{:keys [players
                                              current-player-idx
                                              timeline deck]} g
                                      current-player (get
                                                      players
                                                      current-player-idx)
                                      card (first (:hand current-player))
                                      correct? (logic/check-placement
                                                timeline
                                                card
                                                idx)]
                                  (if correct?
                                    (let [new-hand (vec
                                                    (rest
                                                     (:hand
                                                      current-player)))
                                          winning? (empty? new-hand)
                                          placed-card (if winning?
                                                        (assoc
                                                         card
                                                         :win-highlight?
                                                         true)
                                                        card)
                                          new-timeline (vec
                                                        (concat
                                                         (take
                                                          idx
                                                          timeline)
                                                         [placed-card]
                                                         (drop
                                                          idx
                                                          timeline)))
                                          new-players (assoc-in
                                                       players
                                                       [current-player-idx
                                                        :hand]
                                                       new-hand)]
                                      (assoc g
                                             :timeline new-timeline
                                             :players new-players
                                             :status (if winning?
                                                       :won
                                                       :playing)
                                             :last-result {:correct? true
                                                           :card placed-card
                                                           :winner
                                                           (when winning?
                                                             current-player)}))
                                    (let [new-card (first deck)
                                          new-deck (vec (rest deck))
                                          new-hand (-> current-player
                                                       :hand
                                                       rest
                                                       vec
                                                       (conj new-card))
                                          new-players (assoc-in
                                                       players
                                                       [current-player-idx
                                                        :hand]
                                                       new-hand)]
                                      (assoc
                                       g
                                       :deck new-deck
                                       :players new-players
                                       :last-result {:correct? false
                                                     :card card})))))))
                           :next-turn
                           (set-game
                            (fn [g]
                              (assoc
                               g
                               :current-player-idx
                               (mod
                                (inc (:current-player-idx g))
                                (count (:players g)))
                               :last-result nil)))
                           :restart
                           (set-game nil)))})))))

;; --- Entry Point ---

(defonce root-atom (atom nil))

(defn init! []
  (let [el (js/document.getElementById "app")]
    (when (and el (not @root-atom))
      (reset! root-atom (rdom/createRoot el))))
  (when @root-atom
    (.render ^js @root-atom ($ App))))

(defn ^:before-load stop! []
  (when @root-atom
    (.unmount ^js @root-atom)
    (reset! root-atom nil)))

(defn ^:after-load reload! []
  (init!))
