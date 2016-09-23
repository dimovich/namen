(ns namen.bootstrap
  (:require [reagent.core :as r]
            [cljsjs.react-bootstrap]
            [cljsjs.react-sticky]))


(defn bootstrap [& args]
  (r/adapt-react-class (apply aget js/ReactBootstrap args)))

(def button (bootstrap "Button"))
(def navbar (bootstrap "Navbar"))
(def navbar-form (bootstrap"Navbar" "Form"))
(def form-control (bootstrap "FormControl"))
(def form-group (bootstrap "FormGroup"))
(def form (bootstrap "Form"))
(def control-label (bootstrap "ControlLabel"))
(def grid (bootstrap "Grid"))
(def row (bootstrap "Row"))
(def col (bootstrap "Col"))
(def page-header (bootstrap "PageHeader"))
(def panel (bootstrap "Panel"))
(def input-group (bootstrap "InputGroup"))
(def input-group-button (bootstrap "InputGroup" "Button"))
(def fade (bootstrap "Fade"))
(def table (bootstrap "Table"))
(def stickycontainer (r/adapt-react-class (aget js/ReactSticky "StickyContainer")))
(def sticky (r/adapt-react-class (aget js/ReactSticky "Sticky")))


(defn loading-button [{:keys [state state-key on-click etext dtext opts]}]
  (let [loading (r/atom false)]
    (add-watch state state-key (fn [k r o n]
                                (when-not (= (k o) (k n))
                                  (reset! loading false))))
    (fn []
      [:div
       [button (assoc opts
                      :bs-style "primary"
                      :disabled @loading
                      :type "button"
                      :on-click (fn []
                                  (when-not @loading
                                    (do
                                      (reset! loading true)
                                      (on-click)))))
        (if @loading
          dtext
          etext)]])))
