(defproject relaggregator "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :source-paths ["src/clojure"]
  :main relaggregator.core
 :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.10.0"] 
                 [org.clojure/core.async "1.5.648"]]
  :javac-options ["-target" "17" "-source" "17"]
  :repl-options {:init-ns relaggregator.core})
