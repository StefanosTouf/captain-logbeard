(ns relaggregator.core
  (:gen-class)
  (:require
    [clojure.core.async
     :as a
     :refer [>!! go >! chan pipeline]]
    [relaggregator.config :as conf]
    [relaggregator.database :as db]
    [relaggregator.filter :as f]
    [relaggregator.process :as p])
  (:import
    (relaggregator.LogServer
      LogServer)))


(defn to-db-pipeline
  [config conn]
  (let [in  (chan 1)
        to-db (db/to-db config conn)
        record-to-insert #(p/record-to-insert-columns
                            config %)
        custom-field #(p/custom-field-gen
                        config %)
        pred-vec     (f/get-pred-vec config)
        should-pass? #(f/should-pass? pred-vec %)
        process (comp
                  (map p/syslog-to-record)
                  (map custom-field)
                  (filter should-pass?)
                  (map record-to-insert))]
    (pipeline 4 to-db process in)
    [in to-db]))


;; (defn to-metrics-pipeline
;;   []
;;   (let [in (chan)
;;         out (p/metrics-processor)
;;         process (map p/syslog-to-record)]
;;     (pipeline 2 out process in)
;;     in))


(defn start
  [{:keys [null-retries port] :as config} reader logserver conn
   process-ch db-ch]
  (loop [msg (.readLine reader) nils 0]
    (cond
      (> nils null-retries) (do (println "Too many nulls, restarting...")
                                (>!! db-ch :relaggregator.database/send)
                                (.close logserver)
                                (let [new-logserver (new LogServer port)
                                      new-reader    (.getReader logserver)]
                                  (start config new-reader
                                         new-logserver conn
                                         process-ch db-ch)))
      msg (do (go (>! process-ch msg))
              (recur (.readLine reader) 0))
      :else (recur (.readLine reader)
                   (+ nils 1)))))


(defn -main
  [& _args]
  (let [{:keys [port]
         :as config}  (conf/config)
        conn          (db/conn config)
        _             (db/init-db config conn)
        logserver     (new LogServer port)
        reader        (.getReader logserver)
        [process-ch
         db-ch]    (to-db-pipeline config conn)]
    (start config reader logserver conn process-ch db-ch)))


