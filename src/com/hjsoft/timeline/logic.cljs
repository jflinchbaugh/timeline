(ns com.hjsoft.timeline.logic)

(defn parse-date-val
  "Converts a date string (YYYY, YYYY-MM, or YYYY-MM-DD, optional BC/BCE/AD/CE)
   to a comparable numerical value."
  [date-str]
  (let [matches (re-find
                 #"(\d+)(?:-(\d+))?(?:-(\d+))?\s*(BC|BCE|AD|CE)?"
                 date-str)
        [_ y m d suffix] matches
        year (js/parseInt y 10)
        month (if m (js/parseInt m 10) 1)
        day (if d (js/parseInt d 10) 1)
        bc? (or (= suffix "BC") (= suffix "BCE"))]
    (if bc?
      ;; BC dates: 3000 BC is before 2999 BC.
      ;; We use 13 for months and 416 (13*32) for days to ensure
      ;; they weigh less than a single year unit.
      (+ (- year) (/ month 13) (/ day 416))
      (+ year (/ month 13) (/ day 416)))))

(defn shuffle-deck [deck]
  (shuffle (vec deck)))

(defn- create-player [idx name dealer-deck]
  {:id idx
   :name name
   :hand (take 10 (drop (* idx 10) dealer-deck))})

(defn init-game [events player-names]
  (let [shuffled (shuffle-deck events)
        initial-card (first shuffled)
        dealer-deck (rest shuffled)
        players (vec (map-indexed (fn [idx name]
                                    (create-player idx name dealer-deck))
                                  player-names))
        deck (drop (* (count player-names) 10) dealer-deck)]
    {:timeline [initial-card]
     :players players
     :current-player-idx 0
     :deck deck
     :initial-deck-size (inc (count dealer-deck))
     :status :playing
     :message nil
     :last-result nil}))

(defn check-placement [timeline card index]
  (let [card-val (parse-date-val (:date card))
        before (if (> index 0)
                 (parse-date-val (:date (get timeline (dec index))))
                 nil)
        after (if (< index (count timeline))
                (parse-date-val (:date (get timeline index)))
                nil)]
    (and (or (nil? before) (<= before card-val))
         (or (nil? after) (>= after card-val)))))

(defn- handle-correct-placement [game card index]
  (let [{:keys [players current-player-idx timeline]} game
        current-player (get players current-player-idx)
        new-hand (vec (rest (:hand current-player)))
        placed-card (assoc card :correct-highlight? true)
        new-timeline (vec (concat (take index timeline)
                                  [placed-card]
                                  (drop index timeline)))]
    (assoc game
           :timeline new-timeline
           :players (assoc-in players [current-player-idx :hand] new-hand)
           :last-result {:correct? true :card placed-card})))

(defn- handle-wrong-placement [game card]
  (let [{:keys [players current-player-idx deck]} game
        current-player (get players current-player-idx)
        new-card (first deck)
        new-deck (vec (rest deck))
        new-hand (if (empty? deck)
                   (vec (rest (:hand current-player)))
                   (-> current-player :hand rest vec (conj new-card)))]
    (assoc game
           :deck new-deck
           :players (assoc-in players [current-player-idx :hand] new-hand)
           :last-result {:correct? false :card card})))

(defn- calculate-winners [players deck]
  (let [winning-player (some #(when (empty? (:hand %)) %) players)
        deck-empty? (empty? deck)]
    (cond
      winning-player [winning-player]
      deck-empty? (let [min-cards (apply min (map (comp count :hand) players))]
                    (filterv #(= (count (:hand %)) min-cards) players))
      :else nil)))

(defn- check-game-over [game]
  (let [{:keys [players deck]} game
        winners (calculate-winners players deck)
        game-over? (some? winners)]
    (assoc game
           :status (if game-over? :won :playing)
           :last-result (if game-over?
                          (assoc (:last-result game) :winners winners)
                          (:last-result game)))))

(defn place-card
  "transform game for a card being placed"
  [game placement-index]
  (let [{:keys [players current-player-idx timeline]} game
        current-player (get players current-player-idx)
        card (first (:hand current-player))
        correct? (check-placement timeline card placement-index)
        new-game (if correct?
                   (handle-correct-placement game card placement-index)
                   (handle-wrong-placement game card))]
    (check-game-over new-game)))

(defn next-turn [game]
  (assoc game
         :current-player-idx (mod (inc (:current-player-idx game))
                                  (count (:players game)))
         :timeline (mapv #(dissoc % :correct-highlight? :wrong-highlight?)
                         (:timeline game))
         :last-result nil))
