(ns relaggregator.core
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! go chan pipeline >!! <!! buffer]]
    [clojure.java.jdbc :refer [with-db-connection]]
    [clojure.string :as s]
    [relaggregator.database :refer [insert]])
  (:import
    (java.sql
      Timestamp)
    (java.time
      Instant)
    (relaggregator.LogServer
      LogServer)))


(set! *warn-on-reflection* true)


;; (defn make-syslog-record
;;   [priority version timestamp hostname app-name procid msgid structured-data message]
;;   {:priority priority
;;    :version version
;;    :timestamp timestamp
;;    :hostname hostname
;;    :app_name app-name
;;    :process_id procid
;;    :message_id msgid
;;    :structured-data structured-data
;;    :message message})


(defn type-parser
  [parser value]
  (if (or (= value "") (= value "-")) nil (parser value)))


(defn id
  [v]
  v)


(defn parse-timestamp
  [^String v]
  (Timestamp/from
    (Instant/parse v)))


(defn syslog-to-record
  [log]
  (let
    [[pri-v ts hn an pid msgid sd-msg] (s/split log #" " 7)
     [pri v] (rest (s/split pri-v #"<|>"))
     [str-d-bracket msg] (s/split sd-msg #"- |] " 2)
     str-d (s/replace str-d-bracket #"^\[|]$" "")]
    [(type-parser read-string pri)
     (type-parser read-string v)
     (type-parser parse-timestamp ts)
     (type-parser id  hn)
     (type-parser id an)
     (type-parser read-string pid)
     (type-parser id msgid)
     (type-parser id str-d)
     (type-parser id msg)]))


(defn metrics-processor
  []
  (let [in (chan (buffer 1))]
    (go (while true (str (<! in) "--metrics")))
    in))


(defn printer
  []
  (let [in (chan (buffer 100))]
    (go
        (loop [inserts []]
          (if (= 100 (count inserts))
            (do (insert inserts) (recur [(<! in)]))
            (recur  (conj inserts (<! in))))))
      in))


(def port 5000)


(defn -main
  [& _args]
  (println (str "Started on port: " port))
  (let [metrics-ch (metrics-processor)
        main-ch    (printer)
        reader     (.getReader (new LogServer port))]
    (while true (let [msg (.readLine reader)]
                  (go (->> msg syslog-to-record (>! main-ch)))
                  (go (->> msg syslog-to-record (>! metrics-ch)))))))


