(ns relaggregator.process
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! go chan pipeline >!! <!! buffer]]
    [clojure.string :as s]
    [relaggregator.config :as conf]
    [relaggregator.database :as db])
  (:import
    (java.sql
      Timestamp)
    (java.time
      Instant)))


(set! *warn-on-reflection* true)


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
    {:priority        (type-parser read-string pri)
     :version         (type-parser read-string v)
     :timestamp       (type-parser parse-timestamp ts)
     :hostname        (type-parser id  hn)
     :app_name        (type-parser id an)
     :process_id      (type-parser read-string pid)
     :message_id      (type-parser id msgid)
     :structured_data (type-parser id str-d)
     :message         (type-parser id msg)}))


(defn custom-field-gen
  [syslog-record]
  (let [custom-fields (:custom_fields
                        (conf/table-config))]
    (if custom-fields
      (->>
        (map (fn [[k v]]
               (let [message (syslog-record :message)
                     regex (re-pattern (:regex v))
                     result (re-find regex message)]
                 [k (if (seq? result) (first result) result)])) custom-fields)
        (reduce conj syslog-record))
      syslog-record)))


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
        (let [incoming (<! in)]
          (if (or (= incoming :process/shutdown) (= buff-size (count inserts)))
            (do (db/insert inserts) (recur [incoming]))
            (recur  (conj inserts incoming))))))
    in))
