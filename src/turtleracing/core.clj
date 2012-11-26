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
                       :up [x (dec (dec y))]
                       :down [x (inc (inc y))]
                       :left [(dec (dec x)) y]
                       :right [(inc (inc x)) y])]
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

(defn update-state
  [state {:keys [name command]}]
  (assoc state name
         (control-turtle (get state name turtle) command)))

(def broadcast-ch
  (channel))

(def combined-ch
  (channel))

(receive-all
 combined-ch
 (siphon (reductions* update-state {} combined-ch)
         broadcast-ch))

(defn game-handler
  [ch _]
  (receive ch
           (fn [name]
             (siphon (map* (fn [command]
                             {:command (keyword command)
                              :name name})
                           ch)
                     combined-ch)
             (siphon (map* cheshire/generate-string broadcast-ch) ch))))

(def http-server
  (start-http-server #'game-handler {:port 8008 :websocket true}))
