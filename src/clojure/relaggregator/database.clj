(ns relaggregator.database
  (:require
    [clojure.java.jdbc :as jdbc]
    [jdbc.pool.c3p0 :as pool])
  (:import
    com.mchange.v2.c3p0.ComboPooledDataSource))


(def db-spec
  {:dbtype   "postgresql"
   :dbname   "postgres"
   :user     "postgres"
   :password "postgres"
   :host     "postgres"
   :port      5432})


(def spec
  (pool/make-datasource-spec
    {:classname "org.postgresql.Driver"
     :subprotocol "postgresql"
     :user "postgres"
     :password "postgres"
     :subname "//postgres:5432/postgres"}))


(defn insert
  [inserts]
  (jdbc/insert-multi! spec :logs nil inserts))


(defn init-db
  []
  (let [create-table (jdbc/create-table-ddl :logs
                                            [[:priority        :int]
                                             [:version         :int]
                                             [:timestamp       :timestamp]
                                             [:hostname        :varchar]
                                             [:structured_data :varchar]
                                             [:app_name        :varchar]
                                             [:process_id      :int]
                                             [:message_id      :varchar]
                                             [:message         :varchar]])]
    (jdbc/db-do-commands db-spec [create-table])))

