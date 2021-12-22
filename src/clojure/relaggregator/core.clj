(ns relaggregator.core
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! go chan]]
    [clojure.string :as s])
  (:import
    (relaggregator.LogServer
      LogServer)))


(set! *warn-on-reflection* true)


(defn router-maker
  [channels]
  (fn [msg] (doseq [ch channels] (go (>! ch msg)))))


(defn make-syslog-record
  [priority version timestamp hostname app-name procid msgid structured-data message]
  {:header {:priority priority
            :version version
            :timestamp timestamp
            :hostname hostname
            :app-name app-name
            :process-id procid
            :message-id msgid}
   :structured-data structured-data
   :message message})


(defn parse-structured-data
  [sd-str]
  (let [[id & params] (s/split sd-str #" |=")]
    (apply hash-map
           (cons :sd-id
                 (cons id (loop [[k v & params] params acc ()]
                            (if-not v acc
                                    (recur params (cons (keyword k) (cons v acc))))))))))


(defn syslog-to-record
  [log]
  (let
    [[pri-v ts hn an pid msgid sd-msg] (s/split log #" " 7)
     [pri v] (rest (s/split pri-v #"<|>"))
     [str-d-bracket msg] (s/split sd-msg #"- |] " 1)
     str-d (parse-structured-data
             (s/replace str-d-bracket #"^\[|]$" ""))]
    (make-syslog-record pri v ts hn an pid msgid str-d msg)))


(defn metrics-processor
  []
  (let [in (chan)]
    (go (while true (println (str (<! in) "--metrics"))))
    in))


(defn main-processor
  []
  (let [in (chan)]
    (go (while true (println (syslog-to-record (<! in)))))
    in))


(defn -main
  [& _args]
  (println "Started")
  (let [metrics-ch (metrics-processor)
        main-ch (main-processor)
        port 5000
        reader (.getReader (new LogServer port))
        router (router-maker [metrics-ch main-ch])]
    (println (str "Started on port: " port))
    (while true (router (.readLine reader)))))
