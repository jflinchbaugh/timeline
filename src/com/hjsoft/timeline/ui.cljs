(ns com.hjsoft.timeline.ui
  (:require [helix.core :refer [defnc $]]
            [helix.hooks :as hooks]
            [helix.dom :as d]
            [com.hjsoft.timeline.logic :as logic]))

(defnc Card [{:keys [card revealed? style]}]
  (let [card-clj (if (map? card) card (js->clj card :keywordize-keys true))
        title (or (:title card-clj) "Untitled Event")
        date (or (:date card-clj) "Unknown Date")
        desc (or (:description card-clj) "No description available.")
        win? (:win-highlight? card-clj)
        wrong? (:wrong-highlight? card-clj)]
    (d/div {:class "card" 
            :style (merge {:background "var(--card-bg)" 
                           :border (cond 
                                     win? "4px solid gold"
                                     wrong? "4px solid var(--error)"
                                     :else "2px solid #ccc")
                           :box-shadow (cond
                                         win? "0 0 20px gold"
                                         wrong? "0 0 20px var(--error)"
                                         :else "0 8px 16px rgba(0,0,0,0.1)")
                           :color "#212529"} 
                          style)}
      (d/h2 {:style {:margin-top "0" :font-size "1.8rem"}} title)
      (when revealed?
        (d/div {:style {:animation "fadeIn 0.5s ease-in"}}
          (d/p {:style {:color "#555" :font-size "1.3rem" :margin-bottom "10px" :font-style "italic"}}
               date)
          (d/p {:style {:font-size "1.2rem" :line-height "1.6"}}
               desc))))))

(defnc SetupScreen [{:keys [on-start on-select-data current-file]}]
  (let [[names set-names] (hooks/use-state [""])
        add-player #(set-names conj "")
        valid-names (filter seq (map #(.trim %) names))
        can-start? (seq valid-names)]
    (d/div {:style {:max-width "500px" :margin "0 auto" :text-align "center"}}
      (d/h1 {:style {:color "var(--primary)" :margin-bottom "40px"}} "Timeline Game")
      
      (d/div {:style {:margin-bottom "30px"}}
        (d/label {:style {:display "block" :margin-bottom "10px" :font-weight "bold"}} "Select Theme:")
        (d/select {:value current-file
                   :on-change #(on-select-data (.. % -target -value))
                   :style {:padding "10px" :font-size "1.1rem" :border-radius "8px" :width "100%"}}
          (d/option {:value "history.json"} "World History")
          (d/option {:value "science.json"} "Scientific Discoveries")
          (d/option {:value "inventions.json"} "Inventions")
          (d/option {:value "space.json"} "Space Exploration")))

      (d/div {:style {:margin-top "30px" :margin-bottom "30px"}}
        (d/label {:style {:display "block" :margin-bottom "10px" :font-weight "bold"}} "Players:")
        (for [[idx name] (map-indexed vector names)]
          (d/div {:key idx :style {:margin-bottom "15px"}}
            (d/input {:value name
                      :placeholder (str "Player " (inc idx) " name")
                      :on-change #(let [new-val (.. % -target -value)]
                                    (set-names assoc idx new-val))}))))
      
      (d/div {:style {:display "flex" :gap "15px" :justify-content "center"}}
        (d/button {:on-click add-player :style {:background-color "white" :color "var(--primary)" :border "2px solid var(--primary)"}}
          "+ Add Player")
        (d/button {:on-click #(when can-start? (on-start valid-names))
                   :disabled (not can-start?)
                   :style (when-not can-start? {:opacity 0.5 :cursor "not-allowed"})}
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
      (d/div {:style {:display "flex" :justify-content "center" :gap "20px" :margin-bottom "30px" :flex-wrap "wrap"}}
        (for [p players]
          (let [is-current? (= (:id p) (:id current-player))]
            (d/div {:key (:id p)
                    :style {:padding "10px 20px"
                            :border-radius "10px"
                            :background-color (if is-current? "var(--primary)" "white")
                            :color (if is-current? "white" "black")
                            :box-shadow "0 2px 4px rgba(0,0,0,0.1)"
                            :border (if is-current? "none" "1px solid #ddd")}}
              (d/strong (:name p))
              (d/span {:style {:margin-left "10px"}}
                (str (count (:hand p)) " cards")))))
        (d/div {:style {:padding "10px 20px"
                        :border-radius "10px"
                        :background-color "white"
                        :color "black"
                        :box-shadow "0 2px 4px rgba(0,0,0,0.1)"
                        :border "1px solid #ddd"}}
          (d/strong "Deck: ")
          (d/span {:style {:margin-left "10px"}}
            (str (count deck) " cards"))))

      (d/h2 {:style {:text-align "center" :color "var(--primary)"}}
        (if winner
          (str (:name winner) " Wins!")
          (str (:name current-player) "'s Turn")))

      (when last-result
        (d/div {:class "result-banner"
                :style {:background-color (cond 
                                            winner "gold"
                                            (:correct? last-result) "var(--success)" 
                                            :else "var(--error)")
                        :color (if winner "black" "white")
                        :position "sticky"
                        :top "10px"
                        :z-index "100"}}
          (d/h2 (cond 
                  winner (str "🏆 " (:name winner) " Wins!")
                  (:correct? last-result) "✓ Correct!" 
                  :else "✗ Wrong spot!"))
          (d/p (str "The date was " (:date (:card last-result)) "."))
          (if winner
            (d/button {:on-click #(on-action :restart)
                       :style {:background-color "black" :color "white"}}
              "Play Again")
            (d/button {:on-click #(on-action :next-turn)
                       :style {:background-color "white" :color "black"}}
              (str "Next Player: " (:name next-player))))))

      (d/div
        (when (and (not last-result) (not= status :won) current-card)
          (d/div
            (d/h3 "Your Card:")
            ($ Card {:card current-card :revealed? false})))
        
        (d/h3 "Timeline:")
        (d/div {:style {:display "flex" :flex-direction "column"}}
          (when (and (not last-result) (not= status :won))
            (d/button {:class "place-button" :on-click #(on-action :place 0)}
              "Place here"))
          (for [[idx t-card] (map-indexed vector display-timeline)]
            (d/div {:key idx :ref (if (or (:wrong-highlight? t-card) (:win-highlight? t-card)) scroll-ref nil)}
              ($ Card {:card t-card 
                       :revealed? true})
              (when (and (not last-result) (not= status :won))
                (d/button {:class "place-button" :on-click #(on-action :place (inc idx))}
                  "Place here")))))))))
