(defproject music-mart "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.6.1"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring/ring-mock "0.3.0"]
                 [com.taoensso/carmine "2.14.0"] ; redis
                 [compojure "1.5.1"] ; routing
                 [enlive "1.1.6"] ; scraping
                 [clojurewerkz/quartzite "2.0.0"] ; scheduler
                 [org.clojure/tools.logging "0.3.1"] ; logging
                 [hiccup "1.0.5"] ; html output
                 ]
  :plugins [[environ/environ.lein "0.2.1"]
            [lein-ring "0.9.7"]]
  :ring {:handler music-mart.core/app}
  :hooks [environ.leiningen.hooks]
  :uberjar-name "music-mart-standalone.jar"
  :profiles {:production {:env {:production true}} :uberjar {:aot :all}}
  :main music-mart.core)
