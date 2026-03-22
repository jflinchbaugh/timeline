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

(defn init-game [events player-names]
  (let [shuffled (shuffle-deck events)
        initial-card (first shuffled)
        remaining (rest shuffled)
        players (vec (map-indexed (fn [idx name]
                                    {:id idx
                                     :name name
                                     :hand (take 10 (drop (* idx 10) remaining))})
                                  player-names))
        deck (drop (* (count player-names) 10) remaining)]
    {:timeline [initial-card]
     :players players
     :current-player-idx 0
     :deck deck
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
