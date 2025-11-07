(ns examples.program14)

(defprotocol Drawable
  (draw [this])
  (bounds [this]))

(defrecord Circle [x y radius]
  Drawable
  (draw [this]
    (str "Drawing circle at (" x ", " y ") with radius " radius))
  (bounds [this]
    {:min-x (- x radius)
     :max-x (+ x radius)
     :min-y (- y radius)
     :max-y (+ y radius)}))

(defrecord Rectangle [x y width height]
  Drawable
  (draw [this]
    (str "Drawing rectangle at (" x ", " y ") with width " width " and height " height))
  (bounds [this]
    {:min-x x
     :max-x (+ x width)
     :min-y y
     :max-y (+ y height)}))

(defn -main []
  (let [c (->Circle 10 20 5)
        r (->Rectangle 0 0 30 40)]
    (println (draw c))
    (println (bounds c))
    (println (draw r))
    (println (bounds r))))
