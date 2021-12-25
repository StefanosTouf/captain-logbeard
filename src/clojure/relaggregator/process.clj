(ns relaggregator.process
  (:require
    [clojure.string :as s])
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
    [(type-parser read-string pri)
     (type-parser read-string v)
     (type-parser parse-timestamp ts)
     (type-parser id  hn)
     (type-parser id an)
     (type-parser read-string pid)
     (type-parser id msgid)
     (type-parser id str-d)
     (type-parser id msg)]))

