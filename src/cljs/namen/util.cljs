(ns namen.util
  (:require [reagent.core :as r]))


(defmacro bootstrap [& args]
  `(r/adapt-react-class (aget js/ReactBootstrap ~@args)))
