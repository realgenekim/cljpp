(ns examples.multimethods)

(defmulti render-shape :type)

(defmethod render-shape :circle [shape]
  (str "Circle with radius " (:radius shape)))

(defmethod render-shape :rectangle [shape]
  (str "Rectangle " (:width shape) "x" (:height shape)))

(defmethod render-shape :default [shape]
  {:error "Unknown shape type" :shape shape})
