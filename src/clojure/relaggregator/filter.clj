(ns relaggregator.filter
  (:require
    [relaggregator.config :as conf]
    [relaggregator.macros :as m]
    [relaggregator.process :as p]))


(defn to-db-pipeline
  [log]
  (let [config        (conf/config)
        custom-field #(p/custom-field-gen
                        config %)]
    (->> log
         p/syslog-to-record
         custom-field)))


(def operators
  {:eq =
   :not_eq #(not (= % %))
   ;; gt and lt make more sense as their opposites 
   ;; when considering how the filters are written
   :gte <=
   :lte >=
   :gt <
   :lt >
   :one_of #(boolean (%1 %2))
   :not_one_of #(not (boolean (%1 %2)))})


(defn get-pred-vec
  [{:keys [filters]}]
  (map
    (fn [[field-name predicate-map]]
      (let [[op-k] (keys predicate-map)
            [v]    (vals predicate-map)
            v-set  (if (#{:one_of :not_one_of} op-k)
                     (set v) v)
            op     (operators op-k)]
        [field-name #(m/attempt (op v-set %) true)]))
    filters))


(defn should-pass?
  [pred-vec syslog-record]
  (reduce
    (fn [acc [field-key predicate]]
      (let [test-val (field-key syslog-record)]
        (and acc (predicate test-val))))
    true pred-vec))

