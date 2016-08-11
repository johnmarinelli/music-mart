(ns music-mart.core
  (:require [net.cgrand.enlive-html :as scraper])

  (:use ring.middleware.params
        ring.util.response
        ring.adapter.jetty))

(defn page [name]
  (str  "<html><body>"
        (if name
          (str "Nice to meet you, " name)
          (str "<form>"
               "Name: <input name='name' type='text'></input>"
               "<input type='submit'></input>"
               "</form>"))
        "</body></html>"))

(def url "http://www.935kday.com/playlist/")
(def selector [:ol.amp-recently-played])

(defn fetch-html [url]
  (scraper/html-resource (java.net.URL. url)))

(defn handler [{{name "name"} :params}]
  (-> (response (page name))
      (content-type "text/html")))

(def app 
  (-> handler wrap-params))

(defonce server (run-jetty app {:port 3003 :join? false}))

;(.start server)
;(while true '())
;(.stop server)


