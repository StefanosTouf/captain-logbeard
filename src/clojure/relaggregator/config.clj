(ns relaggregator.config)

(def config {:port            5000
             :logs-per-write  100
             :null-retries    20
             :dbname         "postgres"
             :user           "postgres"
             :password       "postgres"
             :host           "postgres"
             :dbport          5432
             :table-name     :logs})

