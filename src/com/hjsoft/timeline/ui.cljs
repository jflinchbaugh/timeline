(ns com.hjsoft.timeline.ui
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [clojure.string :as str]
            [com.hjsoft.timeline.logic :as logic]))

(defn- save-names [names]
  (let [to-save (filterv #(not (str/blank? %)) names)]
    (js/localStorage.setItem "timeline-player-names"
      (js/JSON.stringify (clj->js to-save)))))

(defn- load-names []
  (try
    (let [stored (js/localStorage.getItem "timeline-player-names")]
      (if (and stored (not= stored "undefined"))
        (js->clj (js/JSON.parse stored))
        [""]))
    (catch :default _ [""])))

(defnc card [{:keys [card revealed? style class-name]}]
  (let [card-clj (if (map? card) card (js->clj card :keywordize-keys true))
        title (or (:title card-clj) "Untitled Event")
        date (or (:date card-clj) "Unknown Date")
        desc (or (:description card-clj) "No description available.")
        correct? (:correct-highlight? card-clj)
        wrong? (:wrong-highlight? card-clj)]
    (d/div {:class (str "card"
                        (when correct? " correct")
                        (when wrong? " wrong")
                        (when class-name (str " " class-name)))
            :style style}
           (d/h2 title)
           (when revealed?
             (d/div {:class "revealed-content"}
                    (d/p {:class "date"} date)
                    (d/p {:class "description"} desc))))))

(defnc setup-screen [{:keys [on-start on-select-data current-file datasets]}]
  (let [[names set-names] (hooks/use-state (load-names))
        input-refs (hooks/use-ref {})
        mounted? (hooks/use-ref false)
        add-player (fn [] (set-names conj ""))
        handle-name-change (fn [idx event]
                             (let [new-val (.. event -target -value)]
                               (set-names assoc idx new-val)))
        handle-theme-change (fn [event]
                              (on-select-data (.. event -target -value)))
        valid-names (filter seq (map str/trim names))
        can-start? (seq valid-names)
        handle-start (fn []
                       (when can-start?
                         (save-names names)
                         (on-start valid-names)))]
    (hooks/use-effect [(count names)]
                      (if-not @mounted?
                        (reset! mounted? true)
                        (let [last-idx (dec (count names))]
                          (when-let [input (get @input-refs last-idx)]
                            (.focus input)))))
    (d/div {:class "setup-screen"}
           (d/h1
             (d/a {:href js/window.location.pathname
                   :style {:text-decoration "none" :color "inherit"}}
               "Timeline"))

           (d/div {:class "field-group"}
                  (d/label "Select Theme:")
                  (d/select {:value current-file
                             :on-change handle-theme-change}
                            (for [dataset datasets]
                              (d/option {:key (:filename dataset)
                                         :value (:filename dataset)}
                                        (:name dataset)))))

           (d/div {:class "field-group"}
                  (d/label "Players:")
                  (for [[idx name] (map-indexed vector names)]
                    (d/div {:key idx :class "player-input-wrapper"}
                           (d/input {:ref #(swap! input-refs assoc idx %)
                                     :value name
                                     :placeholder (str "Player "
                                                    (inc idx)
                                                    " name")
                                     :on-change #(handle-name-change idx %)}))))

           (d/div {:class "button-group"}
                  (d/button {:on-click add-player :class "button-secondary"}
                            "+ Add Player")
                  (d/button {:on-click handle-start
                             :disabled (not can-start?)}
                            "Start Game"))

           (d/div {:class "instructions"}
                  (d/h3 "How to Play")
                  (d/ul
                   (d/li "Place cards in chronological order on the timeline.")
                   (d/li "If correct, the card stays. If wrong, it's discarded.")
                   (d/li "First player to empty their hand wins!"))))))

(defnc game-screen [{:keys [game dataset-name source-url on-action]}]
  (let [{:keys [timeline
                players
                current-player-idx
                last-result
                status
                deck
                initial-deck-size]} game
        current-player (get players current-player-idx)
        next-player-idx (mod (inc current-player-idx) (count players))
        next-player (get players next-player-idx)
        current-card (or (:card last-result) (first (:hand current-player)))
        winners (or (:winners last-result)
                    (when-let [w (:winner last-result)] [w])
                    (:winners game)
                    (when-let [w (:winner game)] [w]))
        game-over? (or (seq winners) (= status :won))

        ;; Calculate the correct index for the card in the timeline
        before-card? (fn [card-val timeline-card]
                       (< (logic/parse-date-val (:date timeline-card))
                          card-val))
        correct-idx (when (and last-result (not (:correct? last-result)))
                      (let [card-val (logic/parse-date-val
                                      (:date current-card))]
                        (count
                         (filter
                          #(before-card? card-val %)
                          timeline))))

        ;; Temporary timeline for rendering wrong result
        display-timeline (if (and last-result (not (:correct? last-result)))
                           (vec (concat (take correct-idx timeline)
                                        [(assoc
                                          current-card
                                          :wrong-highlight?
                                          true)]
                                        (drop correct-idx timeline)))
                           timeline)

        scroll-ref (hooks/use-ref nil)

        handle-restart (fn [] (on-action :restart))
        handle-next-turn (fn [] (on-action :next-turn))
        handle-place (fn [idx] (on-action :place idx))]

    (hooks/use-effect [last-result current-player-idx]
                      (if last-result
                        (js/setTimeout
                         #(when scroll-ref.current
                            (.scrollIntoView
                             scroll-ref.current
                             #js {:behavior "smooth" :block "center"}))
                         50)
                        (.scrollTo js/window #js {:top 0 :behavior "smooth"})))

    (d/div
     (when dataset-name
       (d/div {:style {:text-align "center"
                       :margin-bottom "15px"
                       :font-size "1.1rem"
                       :font-weight "bold"
                       :opacity 0.8}}
              dataset-name))
      ;; Scoreboard
     (d/div {:class "scoreboard"}
            (for [p players]
              (let [is-current? (= (:id p) (:id current-player))]
                (d/div {:key (:id p)
                        :class (str "scoreboard-item"
                                    (when is-current? " current"))}
                       (d/strong (:name p))
                       (d/span {:class "count"}
                               (str (count (:hand p)) " cards")))))
            (d/div {:class "scoreboard-item"}
                   (d/strong "Deck: ")
                   (d/span {:class "count"}
                           (str (count deck) "/" initial-deck-size)))
            (d/button {:on-click handle-restart
                       :class "button-secondary"
                       :style {:padding "5px 15px" :font-size "1rem"}}
                      "Restart"))

     (d/h2 {:class "turn-message"}
           (if game-over?
             (let [names (map :name winners)]
               (if (> (count names) 1)
                 (str (str/join " & " names) " Tie!")
                 (str (first names) " Wins!")))
             (str (:name current-player) "'s Turn")))

     (when last-result
       (d/div {:class (str "result-banner "
                           (cond
                             game-over? "win"
                             (:correct? last-result) "correct"
                             :else "wrong"))}
              (d/h2 (cond
                      game-over? (let [names (map :name winners)]
                                   (if (> (count names) 1)
                                     (str "\uD83C\uDFC6 "
                                       (str/join " & " names)
                                       " Tie!")
                                     (str "\uD83C\uDFC6 "
                                       (first names)
                                       " Wins!")))
                      (:correct? last-result) "\u2713 Correct!"
                      :else "\u2717 Wrong!"))
              (when-not (or game-over? (:correct? last-result))
                (d/p "You draw another card."))
              (if game-over?
                (d/div
                 (d/div {:class "final-scores"}
                        (for [p players]
                          (d/p {:key (:id p)}
                               (str (:name p)
                                 ": "
                                 (count (:hand p))
                                 " cards left")))
                        (d/p (d/strong (str "Deck: "
                                         (count deck)
                                         "/"
                                         initial-deck-size))))
                 (d/button {:on-click handle-restart :class "button-black"}
                           "Play Again"))
                (d/button {:on-click handle-next-turn :class "button-white"}
                          (str "Next Player: " (:name next-player))))))

     (when (and (not last-result) (not= status :won) current-card)
       (d/h3 "Your Card:"))
     (when (and (not last-result) (not= status :won) current-card)
       ($ card {:card current-card
                :revealed? false
                :class-name "sticky"
                :style {:margin-top "0"}}))

     (d/h3 "Timeline:")
     (d/div {:class "timeline-container"}
            (when (and (not last-result) (not= status :won))
              (d/button {:class "place-button" :on-click #(handle-place 0)}
                        "Place here"))
            (for [[idx t-card] (map-indexed vector display-timeline)]
              (d/div {:key idx
                      :ref (if (or
                                (:wrong-highlight? t-card)
                                (:correct-highlight? t-card))
                             scroll-ref
                             nil)}
                     ($ card {:card t-card
                              :revealed? true})
                     (when (and (not last-result) (not= status :won))
                       (d/button {:class "place-button"
                                  :on-click #(handle-place (inc idx))}
                                 "Place here")))))
     (d/div {:style {:text-align "center"
                     :margin-top "40px"
                     :padding-bottom "20px"
                     :font-size "0.9rem"
                     :opacity 0.7}}
            (when source-url
              (d/a {:href source-url :target "_blank"} "Source"))))))
