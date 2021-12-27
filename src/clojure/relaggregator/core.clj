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


(defn to-db-pipeline
  [config conn]
  (let [in  (chan)
        out (p/printer config conn)
        record-to-insert #(db/record-to-insert-columns
                            config %)
        custom-field #(p/custom-field-gen
                            config %)
        process (comp
                  (map p/syslog-to-record)
                  (map custom-field)
                  (map record-to-insert))]
    (pipeline 4 out process in)
    in))


(defn to-metrics-pipeline
  []
  (let [in (chan)
        out (p/metrics-processor)
        process (map p/syslog-to-record)]
    (pipeline 2 out process in)
    in))


(let [{p :dbport} (conf/config)]
  p)


(defn -main
  [& _args]
  (let [{retries :null-retries
         port :port
         :as config}  (conf/config)
        conn          (db/conn config)
        to-db-ch      (to-db-pipeline config conn)
        to-metrics-ch (to-metrics-pipeline)
        main-ch       (p/printer config conn)
        _             (db/init-db config conn)
        logserver     (new LogServer port)
        reader        (.getReader logserver)]
    (loop [msg (.readLine reader) nils 0]
      (cond
        (> nils retries) (do (println "Too many nulls, restarting...")
                             (>!! main-ch :process/shutdown)
                             (.close logserver)
                             (apply -main _args))
        msg (do (go (>! to-db-ch msg))
                (go (>! to-metrics-ch msg))
                (recur (.readLine reader) 0))
        :else (recur (.readLine reader)
                     (+ nils 1))))))


