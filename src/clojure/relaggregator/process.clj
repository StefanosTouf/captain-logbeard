(ns relaggregator.process
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! go chan pipeline >!! <!! buffer go-loop]]
    [clojure.string :as s]
    [relaggregator.macros :refer [go-inf attempt]])
  (:import
    (java.sql
      Timestamp)
    (java.time
      Instant)))


(set! *warn-on-reflection* true)


(defn type-parser
  [parser value]
  (if (or (= value "") (= value "-"))
    nil
    (attempt (parser value) nil)))


(defn id
  [v]
  v)


(defn parse-timestamp
  [^String v]
  (Timestamp/from
    (Instant/parse v)))


(defn parse-number
  "Reads a number from a string. Returns nil if not a number."
  [s]
  (when (re-find #"^-?\d+\.?\d*$" s)
    (read-string s)))


(defn syslog-to-record
  [log]
  (let
    [[pri-v ts hn an pid msgid sd-msg] (s/split log #" " 7)
     [pri v] (rest (s/split pri-v #"<|>"))
     [str-d-bracket msg] (s/split sd-msg #"- |] " 2)
     str-d (s/replace str-d-bracket #"^\[|]$" "")]
    {:priority        (type-parser parse-number pri)
     :version         (type-parser parse-number v)
     :timestamp       (type-parser parse-timestamp ts)
     :hostname        (type-parser id  hn)
     :app_name        (type-parser id an)
     :process_id      (type-parser parse-number pid)
     :message_id      (type-parser id msgid)
     :structured_data (type-parser id str-d)
     :message         (type-parser id msg)}))


(def custom-field-type-parsers
  {"int"       parse-number
   "real"      parse-number
   "varchar"   id
   "timestamp" parse-timestamp})


(defn custom-field-gen
  [{custom-fields :custom_fields} syslog-record]
  (let [custom-fields custom-fields]
    (if custom-fields
      (->>
        (map (fn [[k {:keys [regex type]}]]
               (let [message (syslog-record :message)
                     regex   (re-pattern regex)
                     found   (re-find regex message)
                     match   (if (seq? found) (first found) found)
                     result  (type-parser (custom-field-type-parsers type) match)]
                 [k result])) custom-fields)
        (reduce conj syslog-record))
      syslog-record)))


(defn metrics-processor
  []
  (let [in (chan)]
    (go-inf (str (<! in) "--metrics"))
    in))

(defn record-to-insert-columns
  [{field-val-ref :field-val-ref} log-record]
  (map log-record field-val-ref))
