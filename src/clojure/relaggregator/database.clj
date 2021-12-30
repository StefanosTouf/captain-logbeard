(ns relaggregator.database
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! go chan pipeline >!! <!! buffer go-loop timeout]]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as s]
    [jdbc.pool.c3p0 :as pool]))


(defn db-spec
  [{user :user pass :password host :host
    dbp :dbport dbn :dbname}]
  {:dbtype   "postgresql"
   :dbname   dbn
   :user     user
   :password pass
   :host     host
   :port     dbp})


(defn conn
  [{user :user pass :password host :host
    dbp :dbport dbn :dbname}]
  (pool/make-datasource-spec
    {:classname   "org.postgresql.Driver"
     :subprotocol "postgresql"
     :user        user
     :password    pass
     :subname     (str "//" host ":"  dbp "/"  dbn)}))


(def syslog-fields-to-types
  {:priority "INT"
   :version "INT"
   :timestamp "TIMESTAMP"
   :hostname "VARCHAR"
   :app_name "VARCHAR"
   :process_id "INT"
   :message_id "VARCHAR"
   :structured_data "VARCHAR"
   :message "VARCHAR"})


(defn insert
  [{cks :column-keys
    n :table-name} conn inserts]
  (jdbc/insert-multi!
    conn (keyword n) cks inserts))


(defn init-db
  [{field-val-ref :field-val-ref
    column-keys   :column-keys
    table-name    :table-name
    custom-fields :custom-fields} conn]
  (let [column-types  (map #(let [t (syslog-fields-to-types %)]
                              (if t t (:type (custom-fields %))))
                           field-val-ref)
        columns        (map name column-keys)
        create-table   (str "create table if not exists " table-name " ( "
                            (->> (interleave columns column-types)
                                 (partition 2)
                                 (map #(str (first %) " " (second %)))
                                 (s/join ", ")) " );")]
    (jdbc/db-do-commands conn [create-table])))


;; (defn printer
;;   []
;;   (let [in (chan)]
;;     (go (while true (println (<! in))))
;;     in))


(defn to-db
  [{buff-size :logs-per-write
    :as config} conn]
  (let [in (chan (buffer buff-size))]
    (go
      (while true
        (<! (timeout 2000))
        (>! in ::send)))
    (go-loop [ins []]
      (let [incoming (<! in)
            cnt-ins  (count ins)]
        (cond
          (and (> cnt-ins 0) (= incoming ::send))
          (do
            (println "Inserting: " cnt-ins " logs")
            (insert config conn ins)
            (recur []))

          (= incoming ::send)
          (recur [])

          :else
          (recur (conj ins incoming)))))
    in))

