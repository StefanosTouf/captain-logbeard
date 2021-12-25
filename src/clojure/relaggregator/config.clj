(ns relaggregator.config)


(def config
  (let [env-or (fn [n alt]
                 (or (System/getenv n) alt))]
    {:port           (read-string (env-or "PORT"           "5000"))
     :null-retries   (read-string (env-or "NULL_RETRIES"   "20"))
     :dbport         (read-string (env-or "DB_PORT"        "5432"))
     :logs-per-write (read-string (env-or "LOGS_PER_WRITE" "100"))
     :table-name     (keyword     (env-or "DB_TABLE_NAME"  "logs"))
     :dbname         (env-or "DB_NAME"     "postgres")
     :user           (env-or "DB_USER"     "postgres")
     :password       (env-or "DB_PASSWORD" "postgres")
     :host           (env-or "DB_HOST"     "postgres")}))

