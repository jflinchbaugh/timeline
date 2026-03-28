(require '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn parse-date [date-str]
  (let [date-str (str/trim date-str)
        upper-date (str/upper-case date-str)
        bc? (or (str/ends-with? upper-date " BC")
                (str/ends-with? upper-date " BCE"))
        clean-date (str/replace upper-date #"\s*(BC|BCE|AD|CE)$" "")
        parts (str/split clean-date #"-")]
    (try
      (let [year (Long/parseLong (str/trim (first parts)))
            month (if (> (count parts) 1) (Long/parseLong (str/trim (second parts))) 1)
            day (if (> (count parts) 2) (Long/parseLong (str/trim (nth parts 2))) 1)]
        (if bc?
          (+ (* (- year) 10000) (* month 100) day)
          (+ (* year 10000) (* month 100) day)))
      (catch Exception _
        (binding [*out* *err*]
          (println "Warning: could not parse date:" date-str))
        0))))

(defn sort-json-file [file-path]
  (if (.exists (io/file file-path))
    (do
      (println "Sorting" file-path)
      (let [data (json/parse-string (slurp file-path) true)
            sorted-events (sort-by #(parse-date (:date %)) (:events data))
            new-data (assoc data :events sorted-events)
            ;; Cheshire's pretty printer adds a space before the colon.
            ;; We replace " : " with ": " to match the original style.
            json-str (json/generate-string new-data {:pretty true})
            fixed-json (str/replace json-str #"\" : " "\": ")]
        (spit file-path fixed-json)))
    (println "File not found:" file-path)))

(when (seq *command-line-args*)
  (doseq [f *command-line-args*]
    (sort-json-file f)))
