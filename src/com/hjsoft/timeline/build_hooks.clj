(ns com.hjsoft.timeline.build-hooks
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- copy-dir [src dest]
  (let [src-file (io/file src)
        dest-file (io/file dest)]
    (.mkdirs dest-file)
    (doseq [f (.listFiles src-file)]
      (let [target (io/file dest (.getName f))]
        (if (.isDirectory f)
          (copy-dir f target)
          (io/copy f target))))))

(defn- inject-cache-buster [dest-index]
  (let [content (slurp dest-index)
        version (System/currentTimeMillis)
        new-content (-> content
                        (str/replace "css/style.css" (str "css/style.css?v=" version))
                        (str/replace "js/main.js" (str "js/main.js?v=" version)))]
    (spit dest-index new-content)))

(defn copy-resources
  {:shadow.build/stage :flush}
  [build-state & args]
  (println "Build Hook: Copying resources to target/public...")
  (copy-dir "resources/public" "target/public")
  (when (= (:shadow.build/stage build-state) :flush)
    (println "Build Hook: Injecting cache busters (production only)...")
    (inject-cache-buster "target/public/index.html"))
  build-state)
