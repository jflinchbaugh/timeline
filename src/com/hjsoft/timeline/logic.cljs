(ns com.hjsoft.timeline.logic)

(defn parse-date-val
  "Converts a date string (YYYY, YYYY-MM, or YYYY-MM-DD, optional BC)
   to a comparable numerical value."
  [date-str]
  (let [matches (re-find #"(\d+)(?:-(\d+))?(?:-(\d+))?\s*(BC)?" date-str)
        [_ y m d bc?] matches
        year (js/parseInt y 10)
        month (if m (js/parseInt m 10) 1)
        day (if d (js/parseInt d 10) 1)]
    (if bc?
      ;; BC dates: 3000 BC is smaller than 2999 BC.
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
     :initial-deck-size (count dealer-deck)
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

(defn place-card [game index]
  (let [{:keys [players current-player-idx timeline deck]} game
        current-player (get players current-player-idx)
        card (first (:hand current-player))
        correct? (check-placement timeline card index)]
    (if correct?
      (let [new-hand (vec (rest (:hand current-player)))
            winning? (empty? new-hand)
            placed-card (assoc card :correct-highlight? true)
            new-timeline (vec (concat (take index timeline)
                                      [placed-card]
                                      (drop index timeline)))
            new-players (assoc-in players [current-player-idx :hand] new-hand)]
        (assoc game
               :timeline new-timeline
               :players new-players
               :status (if winning? :won :playing)
               :last-result {:correct? true
                             :card placed-card
                             :winner (when winning? current-player)}))
      (let [new-card (first deck)
            new-deck (vec (rest deck))
            new-hand (-> current-player :hand rest vec (conj new-card))
            new-players (assoc-in players [current-player-idx :hand] new-hand)]
        (assoc game
               :deck new-deck
               :players new-players
               :last-result {:correct? false :card card})))))

(defn next-turn [game]
  (assoc game
         :current-player-idx (mod (inc (:current-player-idx game))
                                  (count (:players game)))
         :timeline (mapv #(dissoc % :correct-highlight? :wrong-highlight?)
                         (:timeline game))
         :last-result nil))
