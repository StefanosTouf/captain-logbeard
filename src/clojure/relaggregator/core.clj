(ns relaggregator.core
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! go chan]])
  (:import
    (relaggregator.LogServer
      LogServer)))


(defn router-maker
  [channels]
  (fn [msg] (doseq [ch channels] (go (>! ch msg)))))


(defn metrics-processor []
  (let [in (chan)]
    (go (while true (println (str (<! in) "--metrics"))))
    in))

(defn main-processor []
  (let [in (chan)]
    (go (while true (println (str (<! in) "--main"))))
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
