(ns relaggregator.core
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! go chan pipeline >!! <!! buffer]]
    [relaggregator.config :as conf]
    [relaggregator.database :as db]
    [relaggregator.process :as p])
  (:import
    (relaggregator.LogServer
      LogServer)))


(defn metrics-processor
  []
  (let [in (chan (buffer 1))]
    (go (while true (str (<! in) "--metrics")))
    in))


(defn printer
  []
  (let [buff-size (conf/config :logs-per-write)
        in        (chan (buffer buff-size))]
    (go
      (loop [inserts []]
        (if (= buff-size (count inserts))
          (do (db/insert inserts) (recur [(<! in)]))
          (recur  (conj inserts (<! in))))))
    in))


(defn -main
  [& _args]
  (let [retries      (conf/config :null-retries)
        port         (conf/config :port)
        metrics-ch   (metrics-processor)
        main-ch      (printer)
        logserver    (new LogServer port)
        reader       (.getReader logserver)]
    (loop [msg (.readLine reader) nils 0]
      (cond
        (> nils retries) (do (println "Too many nulls, restarting...")
                             (.close logserver)
                             (-main))
        msg              (do (go (->> msg p/syslog-to-record (>! main-ch)))
                             (go (->> msg p/syslog-to-record (>! metrics-ch)))
                             (recur (.readLine reader) nils))
        :else            (recur (.readLine reader) (+ nils 1))))))


