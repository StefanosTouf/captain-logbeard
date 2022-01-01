(ns relaggregator.config
  (:require
    [clojure.data.json :as json]))


(defn env-config
  []
  (let [env-or (fn [n alt]
                 (or (System/getenv n) alt))]
    {:port           (read-string (env-or "PORT"           "5000"))
     :null-retries   (read-string (env-or "NULL_RETRIES"   "20"))
     :dbport         (read-string (env-or "DB_PORT"        "5432"))
     :dbname         (env-or "DB_NAME"     "postgres")
     :user           (env-or "DB_USER"     "postgres")
     :password       (env-or "DB_PASSWORD" "postgres")
     :host           (env-or "DB_HOST"     "postgres")
     :config-file    (env-or "CONFIG_PATH" "/opt/logbeard/config.json")}))


;; (defn table-config
;;   []
;;   (:table (json/read-json (slurp (:config-file (config))))))


(defn table-config
  []
  (json/read-json "{
                            \"table-name\":\"LOGS\",
                            \"custom_fields\":{
                              \"event\": {
                                  \"regex\": \"^[^:]+\",
                                  \"type\": \"varchar\"},
                               \"ac_message\": {
                                  \"regex\": \"[^:]+$\",
                                  \"type\": \"varchar\"},
                               \"message_num\": {
                                  \"regex\": \"[^ ]+$\",
                                  \"type\": \"int\"},
                               \"last_digit\":{
                                  \"regex\": \"[0-9]$\",
                                  \"type\": \"int\"}
                              },
                           \"filters\": {
                              \"event\": { 
                                  \"one_of\": [\"Warning\", \"Info\"]},
                              \"last_digit\": { 
                                  \"gt\": 3 },
                            \"columns\":{
                              \"priority\":\"priority\",
                              \"timestamp\":\"timestamp\",
                              \"container_name\": \"app_name\",
                              \"event\": \"event\",
                              \"actual_message\": \"ac_message\",
                              \"message_number\": \"message_num\"}
                  }
                  }
                  "))


(defn custom-table-spec
  []
  (let [table-config       (table-config)
        fields             (:fields table-config)
        custom-fields      (:custom_fields table-config)
        column-keys        (keys fields)
        field-val-ref      (map keyword (vals fields))]
    {:column-keys column-keys
     :field-val-ref field-val-ref
     :custom-fields custom-fields}))


(defn config
  []
  (into {} [(env-config) (table-config) (custom-table-spec)]))
