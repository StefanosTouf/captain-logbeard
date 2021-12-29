(ns relaggregator.filter
  (:require
    [relaggregator.config :as conf]
    [relaggregator.process :as p]))


;; filters:
;;  combinators:
;;  general:
;;     - eq
;;     - one_of
;;     - not_eq
;;     - not_one_of
;;  numbers:
;;     - gt   -needs to transform log into num if not already (eg coming from regex of message)
;;     - lt   -needs to transform log into num if not already (eg coming from regex of message)
;;     - gte
;;     - lte
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
  {:filters
   {:event { :one_of ["13" 123 165]}}})


(def operators
  {:eq =
   :not_eq #(not (= %))
   ;; gt and lt make more sense as their opposites 
   ;; when considering how the filters are written
   :gte <= 
   :lte >=
   :gt <
   :lt >
   :one_of #(boolean (%1 %2))
   :not_one_of #(not (boolean (%1 %2)))})


(defn filters-to-pred-vec
  [{:keys [filters]}]
  (map
    (fn [[field-name predicate-map]]
      (let [[op-k] (keys predicate-map)
            [v]    (vals predicate-map)
            v-set  (if (#{:one_of :not_one_of} op-k) 
                     (set v) v)
            op     (operators op-k)]
        [field-name #(op v-set %)]))
    filters))


(defn should-pass?
  [pred-vec syslog-record]
  (reduce
    (fn [acc [field-key predicate]]
      (let [test-val (field-key syslog-record)]
        (and acc (predicate test-val))))
    true pred-vec))


(def sample-log
  "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog 12312 ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] Error: application event log entry -- 1000")


(should-pass? (filters-to-pred-vec filters) (to-db-pipeline sample-log))


