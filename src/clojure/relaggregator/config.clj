(ns relaggregator.config
  (:require
    [clojure.data.json :as json]))


(def config
  (let [env-or (fn [n alt]
                 (or (System/getenv n) alt))]
    {:port           (read-string (env-or "PORT"           "5000"))
     :null-retries   (read-string (env-or "NULL_RETRIES"   "20"))
     :dbport         (read-string (env-or "DB_PORT"        "5432"))
     :logs-per-write (read-string (env-or "LOGS_PER_WRITE" "5"))
     :dbname         (env-or "DB_NAME"     "postgres")
     :user           (env-or "DB_USER"     "postgres")
     :password       (env-or "DB_PASSWORD" "postgres")
     :host           (env-or "DB_HOST"     "postgres")
     :config-file    (env-or "CONFIG_PATH" "/opt/logbeard/config.json")}))


(defn table-config
  []
  (:table (json/read-json (slurp (:config-file config)))))


; (defn table-config
;   []
;   (:table (json/read-json "{
;                           \"table\":{
;                             \"name\":\"LOGS\",
;                             \"custom_fields\":{
;                               \"event\": {
;                                   \"source\": \"message\",
;                                   \"regex\": \"^[^:]+\"
;                               },
;                                \"actual_message\": {
;                                   \"source\": \"message\",
;                                   \"regex\": \"[^:]+$\"
;                               }
;                             },
;                             \"fields\":{
;                               \"priority\":\"priority\",
;                               \"version\": \"version\",
;                               \"timestamp\":\"timestamp\",
;                               \"hostname\": \"hostname\",
;                               \"app_name\": \"app_name\",
;                               \"process_id\": \"process_id\",
;                               \"message_id\": \"message_id\",
;                               \"structured_data\": \"structured_data\",
;                               \"message\": \"message\"
;                               }
;                             }
;                           }")))
