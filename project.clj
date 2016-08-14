(defproject music-mart "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring/ring-mock "0.3.0"]
                 [com.taoensso/carmine "2.14.0"] ; redis
                 [compojure "1.5.1"] ; routing
                 [enlive "1.1.6"] ; scraping
                 [clojurewerkz/quartzite "2.0.0"] ; scheduler
                 [org.clojure/tools.logging "0.3.1"] ; logging
                 ]
  :main music-mart.core)
