(ns turtleracing.core
  (:use lamina.core
        aleph.http)
  (:require [cheshire.core :as cheshire]))

(def track {:width 320
            :height 240})

(defn out-of-bounds?
  [[x y]]
  (not (and (<= 0 x (:width track))
            (<= 0 y (:height track)))))

(defn move
  [[x y :as position] direction]
  (let [new-position (case direction
                       :up [x (dec y)]
                       :down [x (inc y)]
                       :left [(dec x) y]
                       :right [(inc x) y])]
    (if-not (out-of-bounds? new-position)
      new-position
      position)))

(def turtle
  {:position [10 10]
   :direction :down})

(def directions
  {:up [:left :right]
   :right [:up :down]
   :down [:right :left]
   :left [:down :up]})

(defn turn
  [direction old-direction]
  (case direction
    :left ((directions old-direction) 0)
    :right ((directions old-direction) 1)))

(def turn-left
  (partial turn :left))

(def turn-right
  (partial turn :right))

(defn control-turtle
  [turtle command]
  (merge turtle
         (case command
           :forward {:position (move (:position turtle) (:direction turtle))}
           :left {:direction (turn-left (:direction turtle))}
           :right {:direction (turn-right (:direction turtle))})))

(def broadcast-ch
  (channel))

(def state
  (atom {}))

(defn game-handler
  [ch _]
  (let [parsed-ch (channel)]
    (receive ch
             (fn [name]
               (siphon (map* keyword ch) parsed-ch)
               (siphon (map* (fn [command]
                               (swap! state assoc name (control-turtle (get @state name turtle) command)))
                             parsed-ch)
                       broadcast-ch)
               (siphon (map* cheshire/generate-string broadcast-ch) ch)))))

(def http-server
  (start-http-server #'game-handler {:port 8008 :websocket true}))
