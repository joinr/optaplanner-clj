(defproject optaplanner-clj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :source-paths      ["src/clojure"]
  ;:java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.optaplanner/optaplanner-core "8.5.0.Final"]]
  :repl-options {:init-ns optaplanner-clj.core})
