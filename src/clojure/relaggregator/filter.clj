(ns relaggregator.filter
  (:require
    [relaggregator.config :as conf]
    [relaggregator.process :as p]))


;; filters:
;;  combinators:
;;     - or
;;     - and
;;     - not
;;  general:
;;     - eq
;;  numbers:
;;     - gt   -needs to transform log into num if not already (eg coming from regex of message)
;;     - lt   -needs to transform log into num if not already (eg coming from regex of message)
;;   
;;
;; example
;; {
;;  :ac_message {
;;               eq "Error"
;;               }
;;  }


(defn to-db-pipeline
  [log]
  (let [config        (conf/config)
        custom-field #(p/custom-field-gen
                        config %)]
    (->> log
         p/syslog-to-record
         custom-field)))


(def filters
  {:filters {:event {:eq "Error"}}})


(def operators
  {:eq (fn [filter-val message-val]
         (if (number? filter-val)
           (= filter-val (read-string message-val))
           (= filter-val message-val)))
   :gt #(> %1 (read-string %2))
   :lt #(< %1 (read-string %2))})


(defn filters-to-pred-vec
  [{:keys [filters]}]
  (map
    (fn [[field-name predicate-map]]
      (let [[op-k] (keys predicate-map)
            op     (operators op-k)
            [v]    (vals predicate-map)]
        [field-name #(op v %)]))
    filters))


(defn should-pass?
  [pred-vec syslog-record]
  (reduce
    (fn [acc [field-key predicate]]
      (let [test-val (field-key syslog-record)
            _         (println (predicate test-val))]
        (and acc (predicate test-val))))
    true pred-vec))


(def sample-log "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] Erro: application event log entry...")


(should-pass? (filters-to-pred-vec filters) (to-db-pipeline sample-log))


