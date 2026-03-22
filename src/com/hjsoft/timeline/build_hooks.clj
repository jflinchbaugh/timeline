(ns com.hjsoft.timeline.build-hooks
  (:require [clojure.java.io :as io]))

(defn- copy-dir [src dest]
  (let [src-file (io/file src)
        dest-file (io/file dest)]
    (.mkdirs dest-file)
    (doseq [f (.listFiles src-file)]
      (let [target (io/file dest (.getName f))]
        (if (.isDirectory f)
          (copy-dir f target)
          (io/copy f target))))))

(defn copy-resources
  {:shadow.build/stage :flush}
  [build-state & args]
  (println "Build Hook: Copying resources to target/public...")
  (copy-dir "resources/public" "target/public")
  build-state)
