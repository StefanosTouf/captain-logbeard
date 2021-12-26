(ns relaggregator.database
  (:require
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as s]
    [jdbc.pool.c3p0 :as pool]
    [relaggregator.config :as conf]))


(def db-spec
  {:dbtype   "postgresql"
   :dbname   (conf/config :dbname)
   :user     (conf/config :user)
   :password (conf/config :password)
   :host     (conf/config :host)
   :port     (conf/config :dbport)})


(def conn
  (pool/make-datasource-spec
    {:classname   "org.postgresql.Driver"
     :subprotocol "postgresql"
     :user        (conf/config :user)
     :password    (conf/config :password)
     :subname     (str "//" (conf/config :host)
                       ":"  (conf/config :dbport)
                       "/"  (conf/config :dbname))}))


(defn table-name
  []
  (:name (conf/table-config)))


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


(defn custom-table-spec
  []
  (let [table-config  (conf/table-config)
        fields        (:fields table-config)
        columns-keys  (keys fields)
        field-val-ref (map keyword (vals fields))]
    [columns-keys field-val-ref]))


(defn insert
  [inserts]
  (let [[columns-keys] (custom-table-spec)]
    (jdbc/insert-multi! conn (keyword (table-name)) columns-keys inserts)))


(defn record-to-insert-columns
  [log-record]
  (let [[_ field-val-ref] (custom-table-spec)]
    (map log-record field-val-ref)))


(defn init-db
  []
  (let [[column-keys field-val-ref] (custom-table-spec)
        column-types  (map #(let [t (syslog-fields-to-types %)]
                              (if t t "VARCHAR"))
                           field-val-ref)
        columns        (map name column-keys)
        create-table   (str "create table if not exists " (table-name) " ( "
                            (->> (interleave columns column-types)
                                 (partition 2)
                                 (map #(str (first %) " " (second %)))
                                 (s/join ", ")) " );")]
    (jdbc/db-do-commands conn [create-table])))


