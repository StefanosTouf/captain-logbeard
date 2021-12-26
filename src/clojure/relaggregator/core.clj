(ns relaggregator.core
  (:gen-class)
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


(defn -main
  [& _args]
  (let [retries      20
        port         (conf/config :port)
        metrics-ch   (p/metrics-processor)
        main-ch      (p/printer)
        _            (db/init-db)
        logserver    (new LogServer port)
        reader       (.getReader logserver)]
    (loop [msg (.readLine reader) nils 0]
      (cond
        (> nils retries) (do (println "Too many nulls, restarting...")
                             (>!! main-ch :process/shutdown)
                             (.close logserver)
                             (apply -main _args))
        msg              (do (go (->> msg
                                      p/syslog-to-record
                                      db/record-to-insert-columns
                                      (>! main-ch)))
                             (go (->> msg p/syslog-to-record (>! metrics-ch)))
                             (recur (.readLine reader) 0))
        :else            (recur (.readLine reader) (+ nils 1))))))


