(ns examples.protocols)

(defprotocol Drawable
  (draw [this])
  (bounds [this]))

(defrecord Circle [x y radius]
  Drawable
  (draw [_] (str "Drawing circle at (" x "," y ") with radius " radius))
  (bounds [_] {:min-x (- x radius) :max-x (+ x radius)
               :min-y (- y radius) :max-y (+ y radius)}))

(defrecord Rectangle [x y width height]
  Drawable
  (draw [_] (str "Drawing rectangle at (" x "," y ") " width "x" height))
  (bounds [_] {:min-x x :max-x (+ x width)
               :min-y y :max-y (+ y height)}))
