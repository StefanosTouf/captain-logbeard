(ns relaggregator.database
  (:require
    [clojure.core.async
     :as a
     :refer [>! <! go chan pipeline >!! <!! buffer go-loop timeout]]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as s]
    [jdbc.pool.c3p0 :as pool]
    [relaggregator.config :as con]
    [relaggregator.config :as conf]
    [relaggregator.macros :refer [go-inf]]))


(defn db-spec
  [{:keys [user password host dbport dbname]}]
  {:dbtype   "postgresql"
   :dbname   dbname
   :user     user
   :password password
   :host     host
   :port     dbport})


(defn conn
  [{:keys [user password host dbport dbname]}]
  (pool/make-datasource-spec
    {:classname   "org.postgresql.Driver"
     :subprotocol "postgresql"
     :user        user
     :password    password
     :subname     (str "//" host ":"  dbport "/"  dbname)}))


(def syslog-fields-to-types
  {:priority "INT"
   :version "INT"
   :timestamp "TIMESTAMP"
   :hostname "VARCHAR"
   :app-name "VARCHAR"
   :process-id "INT"
   :message-id "VARCHAR"
   :structured-data "VARCHAR"
   :message "VARCHAR"})


(defn insert
  [{:keys [column-keys table-name]} conn inserts]
  (jdbc/insert-multi!
    conn (keyword table-name) column-keys inserts))


(defn init-db
  [{:keys [field-val-ref column-keys table-name custom-fields]} conn]
  (let [column-types  (map #(let [s-type (syslog-fields-to-types %)
                                  c-type (when custom-fields 
                                           (:type (custom-fields %)))]
                              (if s-type s-type c-type))
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
  [config conn]
  (let [in (chan (buffer 50))]
    (go-inf
      (<! (timeout 2000))
      (>! in ::send))
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
          (do
            (println "No logs to insert")
            (recur []))

          :else
          (recur (conj ins incoming)))))
    in))

