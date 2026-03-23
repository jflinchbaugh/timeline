(ns com.hjsoft.timeline.ui
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [com.hjsoft.timeline.logic :as logic]))

(defnc Card [{:keys [card revealed? style class-name]}]
  (let [card-clj (if (map? card) card (js->clj card :keywordize-keys true))
        title (or (:title card-clj) "Untitled Event")
        date (or (:date card-clj) "Unknown Date")
        desc (or (:description card-clj) "No description available.")
        win? (:win-highlight? card-clj)
        wrong? (:wrong-highlight? card-clj)]
    (d/div {:class (str "card"
                        (when win? " win")
                        (when wrong? " wrong")
                        (when class-name (str " " class-name)))
            :style style}
      (d/h2 title)
      (when revealed?
        (d/div {:class "revealed-content"}
          (d/p {:class "date"} date)
          (d/p {:class "description"} desc))))))

(defnc SetupScreen [{:keys [on-start on-select-data current-file]}]
  (let [[names set-names] (hooks/use-state [""])
        add-player #(set-names conj "")
        valid-names (filter seq (map #(.trim %) names))
        can-start? (seq valid-names)]
    (d/div {:class "setup-screen"}
      (d/h1 "Timeline")

      (d/div {:class "field-group"}
        (d/label "Select Theme:")
        (d/select {:value current-file
                   :on-change #(on-select-data (.. % -target -value))}
          (d/option {:value "history.json"} "World History")
          (d/option {:value "science.json"} "Scientific Discoveries")
          (d/option {:value "inventions.json"} "Inventions")
          (d/option {:value "space.json"} "Space Exploration")))

      (d/div {:class "field-group"}
        (d/label "Players:")
        (for [[idx name] (map-indexed vector names)]
          (d/div {:key idx :class "player-input-wrapper"}
            (d/input {:value name
                      :placeholder (str "Player " (inc idx) " name")
                      :on-change #(let [new-val (.. % -target -value)]
                                    (set-names assoc idx new-val))}))))

      (d/div {:class "button-group"}
        (d/button {:on-click add-player :class "button-secondary"}
          "+ Add Player")
        (d/button {:on-click #(when can-start? (on-start valid-names))
                   :disabled (not can-start?)}
          "Start Game")))))

(defnc GameScreen [{:keys [game on-action]}]
  (let [{:keys [timeline players current-player-idx last-result status deck]} game
        current-player (get players current-player-idx)
        next-player-idx (mod (inc current-player-idx) (count players))
        next-player (get players next-player-idx)
        current-card (or (:card last-result) (first (:hand current-player)))
        winner (or (:winner last-result) (when (= status :won) current-player))

        ;; Calculate the correct index for the card in the timeline
        correct-idx (when (and last-result (not (:correct? last-result)))
                      (let [card-val (logic/parse-date-val (:date current-card))]
                        (count (filter #(< (logic/parse-date-val (:date %)) card-val) timeline))))

        ;; Temporary timeline for rendering wrong result
        display-timeline (if (and last-result (not (:correct? last-result)))
                           (vec (concat (take correct-idx timeline)
                                        [(assoc current-card :wrong-highlight? true)]
                                        (drop correct-idx timeline)))
                           timeline)

        scroll-ref (hooks/use-ref nil)]

    (hooks/use-effect [last-result]
      (if last-result
        (when scroll-ref.current
          (.scrollIntoView scroll-ref.current #js {:behavior "smooth" :block "center"}))
        (.scrollTo js/window #js {:top 0 :behavior "smooth"})))

    (d/div
      ;; Scoreboard
      (d/div {:class "scoreboard"}
        (for [p players]
          (let [is-current? (= (:id p) (:id current-player))]
            (d/div {:key (:id p)
                    :class (str "scoreboard-item" (when is-current? " current"))}
              (d/strong (:name p))
              (d/span {:class "count"}
                (str (count (:hand p)) " cards")))))
        (d/div {:class "scoreboard-item"}
          (d/strong "Deck: ")
          (d/span {:class "count"}
            (str (count deck) " cards"))))

      (d/h2 {:class "turn-message"}
        (if winner
          (str (:name winner) " Wins!")
          (str (:name current-player) "'s Turn")))

      (when last-result
        (d/div {:class (str "result-banner "
                        (cond
                          winner "win"
                          (:correct? last-result) "correct"
                          :else "wrong"))}
          (d/h2 (cond
                  winner (str "🏆 " (:name winner) " Wins!")
                  (:correct? last-result) "✓ Correct!"
                  :else "✗ Wrong spot!"))
          (d/p (str "The date was " (:date (:card last-result)) "."))
          (if winner
            (d/button {:on-click #(on-action :restart) :class "button-black"}
              "Play Again")
            (d/button {:on-click #(on-action :next-turn) :class "button-white"}
              (str "Next Player: " (:name next-player))))))

      (when (and (not last-result) (not= status :won) current-card)
        (d/h3 "Your Card:"))
      (when (and (not last-result) (not= status :won) current-card)
        ($ Card {:card current-card
                 :revealed? false
                 :class-name "sticky"
                 :style {:margin-top "0"}}))

      (d/h3 "Timeline:")
      (d/div {:class "timeline-container"}
        (when (and (not last-result) (not= status :won))
          (d/button {:class "place-button" :on-click #(on-action :place 0)}
            "Place here"))
        (for [[idx t-card] (map-indexed vector display-timeline)]
          (d/div {:key idx :ref (if (or (:wrong-highlight? t-card) (:win-highlight? t-card)) scroll-ref nil)}
            ($ Card {:card t-card
                     :revealed? true})
            (when (and (not last-result) (not= status :won))
              (d/button {:class "place-button" :on-click #(on-action :place (inc idx))}
                "Place here"))))))))
