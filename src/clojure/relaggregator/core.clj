(ns relaggregator.core
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! >!! <!! go chan buffer close! thread
             alts! alts!! timeout]])
  (:import
    (relaggregator.LogServer
      LogServer)))


(defn router-maker
  [channels]
  (fn [msg] (doseq [ch channels] (go (>! ch msg)))))


(defn -main
  [& _args]
  (println "Started")
  (let [ch1 (chan)
        ch2 (chan)
        port 5000
        reader (.getReader (new LogServer port))
        router (router-maker [ch1 ch2])]
    (println (str "Started on port: " port))
    (go (while true (println (str (<! ch1) "---ch1"))))
    (go (while true (println (str (<! ch2) "---ch2"))))
    (while true (router (.readLine reader)))))
