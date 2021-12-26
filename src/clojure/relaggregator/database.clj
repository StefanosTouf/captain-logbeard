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


(defn idkyet
  []
  (let [fields        (:fields (conf/table-config))
        columns-keys  (keys fields)
        columns       (map name columns-keys)
        syslog-fields (map keyword (vals fields))
        column-types  (map syslog-fields-to-types syslog-fields)]
    [columns column-types columns-keys syslog-fields]))


(defn insert
  [inserts]
  (let [[_ _ columns-keys] (idkyet)]
    (jdbc/insert-multi! conn (keyword (table-name)) columns-keys inserts)))


(defn record-to-insert-columns
  [log-record]
  (let [[_ _ _ syslog-fields] (idkyet)]
    (map log-record syslog-fields)))


(defn init-db
  []
  (let [[columns column-types] (idkyet)
        create-table   (str "create table if not exists " (table-name) " ( "
                            (->> (interleave columns column-types)
                                 (partition 2)
                                 (map #(str (first %) " " (second %)))
                                 (s/join ", ")) " );")]
    (jdbc/db-do-commands conn [create-table])))

