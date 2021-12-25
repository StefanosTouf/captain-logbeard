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
        drop-table   (str "drop table " (name table-name))
        create-table (jdbc/create-table-ddl
                       table-name
                       [[:priority        :int]
                        [:version         :int]
                        [:timestamp       :timestamp]
                        [:hostname        :varchar]
                        [:app_name        :varchar]
                        [:process_id      :int]
                        [:message_id      :varchar]
                        [:structured_data :varchar]
                        [:message         :varchar]])]
    (jdbc/execute!       spec [drop-table])
    (jdbc/db-do-commands spec [create-table])))


