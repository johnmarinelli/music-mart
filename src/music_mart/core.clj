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
(def selector [:ol.amp-recently-played :li])

(defn fetch-html [url]
  (scraper/html-resource (java.net.URL. url)))

(defn scrape-html-for-songs [html selector cb]
  (let [elements (scraper/select html selector)] 
    (map cb elements)))

(defn each-element [e]
  (let [line (scraper/text e)
        split (str/split line #"\n")
        trimmed (map str/trim split)
        filtered (filter #(> (count %) 0) trimmed)]
    (apply array-map (interleave '(:time :song-title :artist) trimmed))))

(defn go []
  (let [url "http://www.935kday.com/playlist/"
        selector [:ol.amp-recently-played :li]
;        each-element (fn [e] ())
        each-element (fn [e]
        (let [line (scraper/text e)
              split (str/split line #"\n")
              trimmed (map str/trim split)
              filtered (filter #(> (count %) 0) trimmed)]
          (apply array-map (interleave '(:time :song-title :artist) trimmed))))
        songs (scrape-html-for-songs (fetch-html url) selector each-element)
        place-in-database (fn [song] ())]
    (map identity songs)))

(defn handler [{{name "name"} :params}]
  (-> (response (page name))
      (content-type "text/html")))

(def app 
  (-> handler wrap-params))

(defonce server (run-jetty app {:port 3003 :join? false}))

;(.start server)
;(while true '())
;(.stop server)


