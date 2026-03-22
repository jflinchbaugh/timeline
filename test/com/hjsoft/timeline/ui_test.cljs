(ns com.hjsoft.timeline.ui-test
  (:require [cljs.test :refer [deftest is testing]]
            [helix.core :refer [$]]
            [com.hjsoft.timeline.ui :as ui]
            ["@testing-library/react" :as rtl]))

(deftest card-component-test
  (testing "Card rendering"
    (let [card {:title "Test Event" :date "2021" :description "Test Desc"}
          result (rtl/render ($ ui/Card {:card card :revealed? true}))]
      (is (rtl/screen.getByText "Test Event"))
      (is (rtl/screen.getByText "2021"))
      (is (rtl/screen.getByText "Test Desc"))
      (rtl/cleanup))))

(deftest setup-screen-test
  (testing "Setup screen interaction"
    (let [started? (atom false)
          result (rtl/render ($ ui/SetupScreen {:on-start #(reset! started? true)
                                                :current-file "history.json"}))]
      (let [input (rtl/screen.getByPlaceholderText "Player 1 name")
            button (rtl/screen.getByText "Start Game")]
        (rtl/fireEvent.change input #js {:target #js {:value "Alice"}})
        ;; Button should be enabled now
        (rtl/fireEvent.click button)
        (is @started?)
        (rtl/cleanup)))))
