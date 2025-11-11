(ns examples.program14)
(defprotocol Drawable (draw [this]) (bounds [this]))
(defrecord Circle [radius] Drawable (draw [this] (str "Drawing circle with radius " radius)) (bounds [this] {:width (* 2 radius), :height (* 2 radius)}))
(defrecord Rectangle [width height] Drawable (draw [this] (str "Drawing rectangle " width "x" height)) (bounds [this] {:width width, :height height}))