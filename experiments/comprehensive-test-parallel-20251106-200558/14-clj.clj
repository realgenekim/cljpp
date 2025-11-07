(ns examples.program14)

(defprotocol Drawable
  (draw [this])
  (bounds [this]))

(defrecord Circle [x y radius]
  Drawable
  (draw [this]
    (str "Drawing circle at (" x "," y ") with radius " radius))
  (bounds [this]
    {:x (- x radius)
     :y (- y radius)
     :width (* 2 radius)
     :height (* 2 radius)}))

(defrecord Rectangle [x y width height]
  Drawable
  (draw [this]
    (str "Drawing rectangle at (" x "," y ") with width " width " and height " height))
  (bounds [this]
    {:x x
     :y y
     :width width
     :height height}))

(defn -main []
  (let [c (->Circle 10 10 5)
        r (->Rectangle 20 20 30 40)]
    (println (draw c))
    (println "Circle bounds:" (bounds c))
    (println (draw r))
    (println "Rectangle bounds:" (bounds r))))
