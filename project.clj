(defproject io.axrs.cli/fone-dynamics "0.0.1"
  :description "A Fone Dynamics CLI"
  :license "Eclipse Public License - v 2.0"
  :url "https://github.com/axrs/fone-dynamics"
  :main io.axrs.cli.fone-dynamics.core
  :profiles {:uberjar {:aot :all}}
  :dependencies [[cli-matic "0.3.6"]
                 [clj-time "0.15.0"]
                 ;TODO Extract cli-tools to new library
                 [io.axrs.cli/circle-ci "0.0.1"]
                 [com.taoensso/encore "2.108.1"]
                 [io.jesi/backpack "3.4.1"]
                 [org.clojure/clojure "1.10.1"]
                 [org.martinklepsch/clj-http-lite "0.4.1"]])
