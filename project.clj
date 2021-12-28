(defproject relaggregator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :source-paths ["src/clojure"]
  :main relaggregator.core
  ; :aot :all
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.10.0"] 
                 [org.clojure/core.async "1.5.648"]
                 [org.postgresql/postgresql "42.3.1"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [org.clojure/data.json "2.4.0"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.1"]]
  :javac-options ["-target" "17" "-source" "17"])
