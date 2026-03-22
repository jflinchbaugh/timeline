(ns com.hjsoft.timeline.test-runner
  (:require [cljs.test :as test]
            [com.hjsoft.timeline.core-test]
            [com.hjsoft.timeline.ui-test]))

(defn main []
  (test/run-tests 'com.hjsoft.timeline.core-test
                  'com.hjsoft.timeline.ui-test))
