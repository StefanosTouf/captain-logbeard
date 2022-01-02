(ns relaggregator.filter
  (:require
    [relaggregator.macros :as m]))


(def operators
  {:eq =
   :not-eq #(not (= % %))
   ;; gt and lt make more sense as their opposites 
   ;; when considering how the filters are written
   :gte <=
   :lte >=
   :gt <
   :lt >
   :one-of #(boolean (%1 %2))
   :not-one-of #(not (boolean (%1 %2)))})


(defn get-pred-vec
  [{:keys [filters]}]
  (map
    (fn [[field-name predicate-map]]
      (let [[op-k] (keys predicate-map)
            [v]    (vals predicate-map)
            v-set  (if (#{:one-of :not-one-of} op-k)
                     (set v) v)
            op     (operators op-k)]
        [field-name #(m/attempt (op v-set %) true)]))
    filters))


(defn should-pass?
  [pred-vec syslog-record]
  (reduce
    (fn [acc [field-key predicate]]
      (let [test-val (field-key syslog-record)]
        (if test-val
          (and acc (predicate test-val))
          true)))
    true pred-vec))

