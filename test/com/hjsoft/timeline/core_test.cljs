(ns com.hjsoft.timeline.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [com.hjsoft.timeline.logic :as logic]))

(deftest parse-date-val-test
  (testing "Parsing dates with precision"
    (is (< (logic/parse-date-val "3001 BC")
           (logic/parse-date-val "3000 BC")))
    (is (< (logic/parse-date-val "3000-01 BC")
           (logic/parse-date-val "3000-02 BC")))
    (is (< (logic/parse-date-val "30 BC")
           (logic/parse-date-val "29 BC")))
    (is (< (logic/parse-date-val "1969-07-20")
           (logic/parse-date-val "1969-07-21")))
    (is (< (logic/parse-date-val "2021-01")
           (logic/parse-date-val "2021-02")))))

(deftest init-game-test
  (let [events (map #(hash-map :title (str %) :date (str %)) (range 50))
        names ["Alice" "Bob"]
        game (logic/init-game events names)]
    (is (= 2 (count (:players game))))
    (is (= 1 (count (:timeline game))))
    (is (= 10 (count (get-in game [:players 0 :hand]))))
    (is (= 10 (count (get-in game [:players 1 :hand]))))
    (is (= 29 (count (:deck game))))))

(deftest check-placement-test
  (let [timeline [{:date "1000"} {:date "2000"}]]
    (testing "Correct placements"
      (is (logic/check-placement timeline {:date "500"} 0))
      (is (logic/check-placement timeline {:date "1500"} 1))
      (is (logic/check-placement timeline {:date "2500"} 2))
      (is (logic/check-placement timeline {:date "1000"} 0))
      (is (logic/check-placement timeline {:date "1000"} 1)))
    (testing "Incorrect placements"
      (is (not (logic/check-placement timeline {:date "1500"} 0)))
      (is (not (logic/check-placement timeline {:date "500"} 1)))
      (is (not (logic/check-placement timeline {:date "1500"} 2))))
    (testing "Boundary conditions"
      (is (logic/check-placement timeline {:date "1000"} 0))
      (is (logic/check-placement timeline {:date "1000"} 1))
      (is (logic/check-placement timeline {:date "2000"} 1))
      (is (logic/check-placement timeline {:date "2000"} 2)))
    (testing "Precise date placements"
      (let [timeline-precise [{:date "2021-01"} {:date "2021-03"}]]
        (is (logic/check-placement timeline-precise {:date "2021-02"} 1))
        (is (not (logic/check-placement
                   timeline-precise
                   {:date "2021-04"}
                   1)))))))
