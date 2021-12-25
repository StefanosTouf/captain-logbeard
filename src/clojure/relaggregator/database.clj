(ns relaggregator.database
  (:require
    [clojure.java.jdbc :as jdbc]
    [jdbc.pool.c3p0 :as pool]
    [relaggregator.config :as conf]))


(def db-spec
  {:dbtype   "postgresql"
   :dbname   (conf/config :dbname)
   :user     (conf/config :user)
   :password (conf/config :password)
   :host     (conf/config :host)
   :port     (conf/config :dbport)})


(def spec
  (pool/make-datasource-spec
    {:classname   "org.postgresql.Driver"
     :subprotocol "postgresql"
     :user        (conf/config :user)
     :password    (conf/config :password)
     :subname     (str "//" (conf/config :host)
                       ":"  (conf/config :dbport)
                       "/"  (conf/config :dbname))}))


(defn insert
  [inserts]
  (jdbc/insert-multi! spec (conf/config :table-name) nil inserts))


(defn init-db
  []
  (let [table-name   (conf/config :table-name)
        create-table   (str "create table if not exists " (name table-name)
                            "(priority        INT,
                              version         INT,
                              timestamp       TIMESTAMP,
                              hostname        VARCHAR,
                              app_name        VARCHAR,
                              process_id      INT,
                              message_id      VARCHAR,
                              structured_data VARCHAR,
                              message         VARCHAR);")]
    (jdbc/db-do-commands spec [create-table])))


