(ns music-mart.core
  (:require [clojure.tools.logging :as log]
            [net.cgrand.enlive-html :as scraper]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.string :as str]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.jobs :as j]
            [clojurewerkz.quartzite.jobs :refer [defjob]]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.daily-interval :refer [schedule on-every-day
                                                                    starting-daily-at time-of-day ending-daily-at
                                                                    with-interval-in-minutes with-interval-in-hours]])
  (:use ring.middleware.params
        ring.util.response
        ring.adapter.jetty))

(def server1-conn {:pool {} :spec {:uri (get (System/getenv) "REDIS_URL" "redis://localhost:6379")}})
(defmacro wcar* [& body]
  `(car/wcar server1-conn ~@body))

(def redis-key "kday")
(def url "http://www.935kday.com/playlist/")
(def selector [:ol.amp-recently-played :li])

(defn parse-kday-timestamp [ts]
  (let [trimmed (str/trim ts)
        pieces (str/split trimmed #" ")
        midday (str/lower-case (second pieces))
        [hh mm] (map read-string (str/split (first pieces) #":"))
        afternoon (= midday "pm")
        timestamp-format (fn [h m] (format "%02d:%02d" h m))]
    (if afternoon
      (let [new-hh (if (> 12 hh) (+ 12 hh) hh)]
        (timestamp-format new-hh mm))
      (let [new-hh (if (= 12 hh) 0 hh)]
        (timestamp-format new-hh mm)))))

(defn fetch-html [url]
  (scraper/html-resource (java.net.URL. url)))

(defn scrape-html-for-songs [html selector cb]
  (let [elements (scraper/select html selector)] 
    (map cb elements)))

(defn each-element [e]
  (let [selectors [:.amp-recently-played-timestamp :.amp-recently-played-title :.amp-recently-played-artist]
        [ts song-title artist] (map #(first (get (first (scraper/select e [%])) :content)) selectors)]
    (apply array-map (interleave '(:time :song-title :artist) [ts song-title artist]))))

(defn get-kday []
  (let [url "http://www.935kday.com/playlist/"
        selector [:ol.amp-recently-played :li]
        each-element (fn [e]
                       (let [selectors [:.amp-recently-played-timestamp :.amp-recently-played-title :.amp-recently-played-artist]
                             [ts song-title artist] (map #(first (get (first (scraper/select e [%])) :content)) selectors)]
                         (apply array-map (interleave '(:time :song-title :artist) [ts song-title artist]))))
        songs (scrape-html-for-songs (fetch-html url) selector each-element)
        old-list (wcar* (car/lrange redis-key 0 -1))
        place-in-database (fn [{time :time song-title :song-title artist :artist}] 
                            (let [new-time (parse-kday-timestamp time)]
                              (println "Inserting " (str new-time " " song-title " " artist))
                              (wcar* (car/lpush redis-key (str new-time " " song-title " " artist)))
                              (wcar* (car/set new-time (str song-title " " artist)))))]
    (map place-in-database songs)))

(defn clear-redis []
  (wcar* (car/del redis-key))
  (let [keys (wcar* (car/keys "*"))]
    (map #(wcar* (car/del %)) keys)))

(defn page []
  (let [songs-today (wcar* (car/lrange redis-key 0 -1))]
    (map str songs-today)))

(defn handler [{{name "name"} :params}]
  (-> (response (page))
      (content-type "text/html")))

(def app 
  (-> handler wrap-params))

(defonce server (run-jetty app {:port (Integer/parseInt (get (System/getenv) "PORT" "3001")) :join? false}))

;(.start server)
;(while true '())
;(.stop server)

(def ctr (atom 0))
(defjob ScrapeKdayJob [ctx]
  (swap! ctr inc)
  (let [res (get-kday)]
  (spit "test.txt" res :append true)))

(defjob ClearRedisJob [ctx]
  (clear-redis))

(defn -main
  [& m]
  (let [s (-> (qs/initialize) qs/start)
        job (j/build 
             (j/of-type ScrapeKdayJob)
             (j/with-identity (j/key "jobs.kday.1")))
        trigger (t/build
                 (t/with-identity (t/key "triggers.1"))
                 (t/start-now)
                 (t/with-schedule (schedule
                                   (with-interval-in-minutes 1)
                                   (on-every-day)
                                   (starting-daily-at (time-of-day 00 00 01))
                                   (ending-daily-at (time-of-day 23 59 59)))))
        rs (-> (qs/initialize) qs/start)
        clear-redis-job (j/build
                         (j/of-type ClearRedisJob)
                         (j/with-identity (j/key "jobs.clear.2")))
        clear-redis-trigger (t/build
                             (t/with-identity (t/key "triggers.2"))
                             (t/start-now)
                             (t/with-schedule (schedule
                                               (with-interval-in-hours 24)
                                               (on-every-day)
                                               (starting-daily-at (time-of-day 00 00 00)))))
        start? (= "y" (second m))]
    (qs/standby s)
    (qs/standby rs)
    (when start?
          (do
;            (qs/schedule rs clear-redis-job clear-redis-trigger)
;            (qs/schedule s job trigger)
            )))
;  (.start server)
  )
