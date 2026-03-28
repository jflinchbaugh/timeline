(ns com.hjsoft.timeline.ui-test
  (:require [com.hjsoft.timeline.test-env]
            [cljs.test :refer [deftest is testing use-fixtures]]
            [helix.core :refer [$]]
            [com.hjsoft.timeline.ui :as ui]
            ["@testing-library/react" :as rtl]))

(use-fixtures :each
  {:after rtl/cleanup})

(deftest card-component-test
  (testing "Card rendering"
    (let [card {:title "Test Event" :date "2021" :description "Test Desc"}
          _ (rtl/render ($ ui/card {:card card :revealed? true}))]
      (is (rtl/screen.getByText "Test Event"))
      (is (rtl/screen.getByText "2021"))
      (is (rtl/screen.getByText "Test Desc")))))

(deftest setup-screen-test
  (testing "Setup screen interaction"
    (let [started? (atom false)
          _ (rtl/render ($ ui/setup-screen
                                {:on-start #(reset! started? true)
                                 :current-file "history.json"}))
          input (rtl/screen.getByPlaceholderText "Player 1 name")
          button (rtl/screen.getByText "Start Game")]
      (rtl/fireEvent.change input #js {:target #js {:value "Alice"}})
        ;; Button should be enabled now
      (rtl/fireEvent.click button)
      (is @started?))))

(deftest setup-screen-persistence-test
  (testing "Setup screen saves and culls blank names"
    (.clear js/localStorage)
    (let [started? (atom false)
          _ (rtl/render ($ ui/setup-screen
                           {:on-start #(reset! started? true)
                            :current-file "history.json"}))]
      (let [input1 (rtl/screen.getByPlaceholderText "Player 1 name")
            add-button (rtl/screen.getByText "+ Add Player")
            start-button (rtl/screen.getByText "Start Game")]

        ;; Enter "Alice" for Player 1
        (rtl/fireEvent.change input1 #js {:target #js {:value "Alice"}})

        ;; Add Player 2 and leave blank
        (rtl/fireEvent.click add-button)

        ;; Add Player 3 and enter "Bob"
        (rtl/fireEvent.click add-button)
        (let [inputs (rtl/screen.getAllByPlaceholderText #"Player \d name")
              input3 (nth (vec inputs) 2)]
          (rtl/fireEvent.change input3 #js {:target #js {:value "Bob"}}))

        (rtl/fireEvent.click start-button)

        (is @started?)
        (let [stored (js/JSON.parse
                      (.getItem js/localStorage "timeline-player-names"))
              stored-clj (js->clj stored)]
          (is (= ["Alice" "Bob"] stored-clj)
              "only provided names are remembered")))))

  (testing "Setup screen loads names from localStorage"
    (.setItem js/localStorage
              "timeline-player-names"
              (js/JSON.stringify #js ["Charlie" "Dave"]))
    (let [_ (rtl/render ($ ui/setup-screen
                           {:on-start #()
                            :current-file "history.json"}))]
      (is (rtl/screen.getByDisplayValue "Charlie"))
      (is (rtl/screen.getByDisplayValue "Dave")))))

(deftest game-screen-deck-count-test
  (testing "game screen displays deck count"
    (let [game {:timeline [{:title "Event 1" :date "1000"}]
                :players [{:id 0
                           :name "Alice"
                           :hand [{:title "Event 2" :date "2000"}]}]
                :current-player-idx 0
                :deck (repeat 40 {:title "Deck Card" :date "2020"})
                :initial-deck-size 100
                :status :playing}
          _ (rtl/render ($ ui/game-screen {:game game}))]
      (is (rtl/screen.getByText #"Deck:"))
      (is (rtl/screen.getByText "40/100")))))

(deftest game-screen-win-test
  (testing "game screen displays winning banner and final scores"
    (let [game {:timeline [{:title "E1" :date "1000"}]
                :players [{:id 0
                           :name "Alice"
                           :hand []}
                          {:id 1
                           :name "Bob"
                           :hand [{:title "E2" :date "2000"}]}]
                :current-player-idx 0
                :deck []
                :initial-deck-size 21
                :status :won
                :winner {:id 0 :name "Alice"}
                :last-result {:correct? true :winner {:id 0 :name "Alice"}}}
          _ (rtl/render ($ ui/game-screen {:game game}))]
      (is (seq (rtl/screen.getAllByText #"Alice Wins!")))
      (is (rtl/screen.getByText "Alice: 0 cards left"))
      (is (rtl/screen.getByText "Bob: 1 cards left"))
      (is (rtl/screen.getByText "Deck: 0/21")))))

(deftest game-screen-tie-test
  (testing "game screen displays tie banner"
    (let [game {:timeline [{:title "E1" :date "1000"}]
                :players [{:id 0 :name "Alice" :hand [{:title "E2" :date "2000"}]}
                          {:id 1 :name "Bob" :hand [{:title "E3" :date "3000"}]}]
                :current-player-idx 0
                :deck []
                :initial-deck-size 21
                :status :won
                :last-result {:correct? false
                              :card {:title "E4" :date "4000"}
                              :winners [{:id 0 :name "Alice"}
                                        {:id 1 :name "Bob"}]}}
          _ (rtl/render ($ ui/game-screen {:game game}))]
      (is (seq (rtl/screen.getAllByText #"Alice & Bob Tie!"))))))

(deftest game-screen-wrong-test
  (testing "game screen displays wrong banner"
    (let [game {:timeline [{:title "E1" :date "1000"}]
                :players [{:id 0
                           :name "Alice"
                           :hand []}
                          {:id 1
                           :name "Bob"
                           :hand [{:title "E3" :date "3000"}]}]
                :current-player-idx 0
                :status :playing
                :last-result {:correct? false
                              :card {:title "E2" :date "2000"}}}
          _ (rtl/render ($ ui/game-screen {:game game}))]
      (is (rtl/screen.getByText (re-pattern (str ui/cross-mark " Wrong!"))))
      (is (rtl/screen.getByText #"You draw another card."))
      (is (rtl/screen.getByText "Next Player: Bob"))
      ;; Card should be shown in timeline with date revealed
      (is (rtl/screen.getByText "E2"))
      (is (rtl/screen.getByText "2000")))))

(deftest game-screen-restart-test
  (testing "game screen displays restart button and handles click"
    (let [restarted? (atom false)
          game {:timeline [{:title "E1" :date "1000"}]
                :players [{:id 0
                           :name "Alice"
                           :hand [{:title "E2" :date "2000"}]}]
                :current-player-idx 0
                :status :playing
                :deck []}
          _ (rtl/render ($ ui/game-screen
                           {:game game
                            :on-action (fn [action]
                                         (when (= action :restart)
                                           (reset! restarted? true)))}))]
      (let [button (rtl/screen.getByText "Restart")]
        (rtl/fireEvent.click button)
        (is @restarted?)))))

(deftest game-screen-source-link-test
  (testing "game screen displays source link when source-url is provided"
    (let [game {:timeline [{:title "E1" :date "1000"}]
                :players [{:id 0
                           :name "Alice"
                           :hand [{:title "E2" :date "2000"}]}]
                :current-player-idx 0
                :status :playing
                :deck []}
          source-url "https://example.com/source"
          _ (rtl/render ($ ui/game-screen
                           {:game game
                            :source-url source-url}))]
      (let [link (rtl/screen.getByText "Data Source")]
        (is (= (.getAttribute link "href") source-url))
        (is (= (.getAttribute link "target") "_blank"))))))

(deftest game-screen-github-link-test
  (testing "game screen displays github project link"
    (let [game {:timeline [{:title "E1" :date "1000"}]
                :players [{:id 0
                           :name "Alice"
                           :hand [{:title "E2" :date "2000"}]}]
                :current-player-idx 0
                :status :playing
                :deck []}
          _ (rtl/render ($ ui/game-screen {:game game}))]
      (let [link (rtl/screen.getByText "GitHub")]
        (is (= (.getAttribute link "href") "https://github.com/jflinchbaugh/timeline"))
        (is (= (.getAttribute link "target") "_blank"))))))
