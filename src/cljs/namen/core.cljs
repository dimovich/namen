(ns namen.core
  (:require [reagent.core :as r :refer [render]]
            [domina.core :refer [value by-id]]
            [domina.events :refer [listen! prevent-default]]
            [cljs.reader :refer [read-string]]
            [clojure.string :as s :refer [blank?]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]]
            [cljsjs.react-bootstrap])
  
  (:require-macros [shoreleave.remotes.macros :as macros]))


;;word, weight
(def data (r/atom {:results []
                   :loading false
                   :results-visible false}))

(defn word-component [word weight]
  [:div#word.word word])


(defn word-list [data]
  [:div.results
   (for [[word weight] (:results @data)]
     ^{:key word} [word-component word weight])])


(defn handle-words [words]
  (let [ws (re-seq #"\w+" words)]
    (println "calling remote...")
    (remote-callback :generate
                     [ws 33]
                     #(swap! data assoc :results  % :loading false))))








(defn loading-button [f id l nl]
  (let [loading (r/atom false)]
    (fn []
      [:div
       [button {:id id
                :bs-style "primary"
                :disabled (:loading @data)
                :on-click (fn []
                            (when-not (:loading @data)
                              (do
                                (swap! data assoc :loading true)
                                (f))))}
        (if (:loading @data)
          l
          nl)]])))


(defn word-form []
  (let [text (r/atom "")]
    (fn []
      [:div
       [:label {:for "words-input"} "Words:"]
       [:input.words-input {:type "text"
                            :placeholder "Enter words..."
                            :value @text
                            :on-change #(reset! text (-> % .-target .-value))
                            :on-key-press (fn [e]
                                            (when (= 13 (.-charCode e))
                                              (.click (by-id "generate"))))}]

       [loading-button #(when-not (blank? @text)
                          (handle-words @text)) "generate" "Generating..." "Generate"]])))


(comment [:button.generate-button {:on-click #(handle-words @text)}
          "Generate"])


(def button (r/adapt-react-class (aget js/ReactBootstrap "Button")))
(def navbar (r/adapt-react-class (aget js/ReactBootstrap "Navbar")))
(def navbar-form (r/adapt-react-class (aget js/ReactBootstrap "Navbar" "Form")))
(def form-control (r/adapt-react-class (aget js/ReactBootstrap "FormControl")))
(def form-group (r/adapt-react-class (aget js/ReactBootstrap "FormGroup")))
(def form (r/adapt-react-class (aget js/ReactBootstrap "Form")))
(def control-label (r/adapt-react-class (aget js/ReactBootstrap "ControlLabel")))
(def grid (r/adapt-react-class (aget js/ReactBootstrap "Grid")))
(def row (r/adapt-react-class (aget js/ReactBootstrap "Row")))
(def col (r/adapt-react-class (aget js/ReactBootstrap "Col")))
(def page-header (r/adapt-react-class (aget js/ReactBootstrap "PageHeader")))
(def panel (r/adapt-react-class (aget js/ReactBootstrap "Panel")))
(def input-group (r/adapt-react-class (aget js/ReactBootstrap "InputGroup")))
(def input-group-button (r/adapt-react-class (aget js/ReactBootstrap "InputGroup" "Button")))
(def fade (r/adapt-react-class (aget js/ReactBootstrap "Fade")))




(defn main-form [state]
  [grid
   [row
    [col {:md 8}
     [page-header  "Namen Ramen Generator"]]]
   [row
    [col {:md 8}
     [form
      [form-group
       [input-group
        [form-control {:type "text" :placeholder "insert keywords"}]
        [input-group-button
         [button {:type "submit"}
          "Generate"]]]]]]]
   [fade
    [row
     [col {:md 8}
      [panel {:header "Google"}]]]

    [row
     [col {:md 8}
      [panel {:header "Wikipedia"}]]]

    [row
     [col {:md 8}
      [panel {:header "Thesaurus"}]]]]])


(defn box [state]
  [:div
   [main-form state]])



(defn ^:export init []
  (enable-console-print!)
  (when (and js/document
             (aget js/document "getElementById"))
    (render [box data] (by-id "app"))))
