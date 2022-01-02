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





(defn table-config
  []
  (json/read-json (slurp (:config-file (env-config)))))


;; (defn table-config
;;   []
;;   (json/read-json "{
;;                             \"table-name\":\"LOGS\",
;;                             \"custom-fields\":{
;;                               \"event\": {
;;                                   \"regex\": \"^[^:]+\",
;;                                   \"type\": \"varchar\"},
;;                                \"ac-message\": {
;;                                   \"regex\": \"[^:]+$\",
;;                                   \"type\": \"varchar\"},
;;                                \"message-num\": {
;;                                   \"regex\": \"[^ ]+$\",
;;                                   \"type\": \"int\"},
;;                                \"last-digit\":{
;;                                   \"regex\": \"[0-9]$\",
;;                                   \"type\": \"int\"}
;;                               },
;;                            \"filters\": {
;;                               \"event\": {
;;                                   \"one-of\": [\"Warning\", \"Info\"]},
;;                               \"last-digit\": {
;;                                   \"gt\": 3 }},
;;                             \"columns\":{
;;                               \"pri\":\"priority\",
;;                               \"timestamp\":\"timestamp\",
;;                               \"container_name\": \"app-name\",
;;                               \"event\": \"event\",
;;                               \"actual_message\": \"ac-message\",
;;                               \"message_number\": \"message-num\"}}"))


(defn custom-table-spec
  []
  (let [{:keys [columns
                custom-fields]}  (table-config)
        column-keys   (keys columns)
        field-val-ref (map keyword (vals columns))]
    {:column-keys   column-keys
     :field-val-ref field-val-ref
     :custom-fields custom-fields}))


(defn config
  []
  (into {} [(env-config) (table-config) (custom-table-spec)]))
