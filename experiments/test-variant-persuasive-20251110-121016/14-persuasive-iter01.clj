(ns examples.program14)
(defprotocol Drawable (draw [this]) (bounds [this]))
(defrecord Circle [radius] Drawable (draw [this] (str "Drawing circle with radius " (:radius this))) (bounds [this] {:width (* 2 (:radius this)), :height (* 2 (:radius this))}))
(defrecord Rectangle [width height] Drawable (draw [this] (str "Drawing rectangle " (:width this) "x" (:height this))) (bounds [this] {:width (:width this), :height (:height this)}))