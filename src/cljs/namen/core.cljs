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
(def data (r/atom {:results {:google []
                             :thesaurus []
                             :wikipedia []}
                   :results-visible true}))




(defn handle-words [words]
  (let [ws (re-seq #"\w+" words)]
    (remote-callback :generate
                     [ws 33]
                     #(swap! data assoc-in [:results :google] %))))


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


(defn loading-button [{:keys [state on-click etext dtext opts]}]
  [:div
   [button (assoc opts
                  :bs-style "primary"
                  :disabled (@state)
                  :type "button"
                  :on-click (fn []
                              (when-not @state
                                (do
                                  (reset! state true)
                                  (on-click)))))
    (if @state
      dtext
      etext)]])



(defn input-form []
  (let [text (r/atom "")
        loading (r/atom false)]
    (fn []
      [form-group
       [input-group
        [form-control {:type "text"
                       :placeholder "insert keywords"
                       :value @text
                       :on-change #(reset! text (-> % .-target .-value))
                       :on-submit #(prevent-default %) ;; do we need this?
                       :on-key-press (fn [e]
                                       (when (= 13 (.-charCode e))
                                         (.click (by-id "generate"))))}]
        [input-group-button
         [loading-button {:etext "Generate"
                          :dtext "Generating..."
                          :opts {:id "generate"}
                          :state loading
                          :on-click #(handle-words @text)}]]]])))


(defn word-component [word weight]
  [:div.word
   word])


(defn word-list [data]
  [:div.3columns
   (for [[word weight] data]
     ^{:key word} [word-component word weight])])



(defn main-form [state]
  [grid
   [row
    [col {:md 8}
     [page-header  "Namen Ramen Generator"]]]
   [row
    [col {:md 8}
     [input-form]]]
   
   [fade {:in (:results-visible @state)}
    [row
     [col {:md 12}
      [row
       [col {:md 8}
        [panel {:header "Google"
                :collapsible true
                :default-expanded true}
         [word-list (get-in @state [:results :google])]]]]
      [row
       [col {:md 8}
        [panel {:header "Wikipedia"
                :collapsible true
                :default-expanded true}]]]

      [row
       [col {:md 8}
        [panel {:header "Thesaurus"
                :collapsible true
                :default-expanded true}]]]]]]])


(defn box [state]
  [:div
   [main-form state]])



(defn ^:export init []
  (enable-console-print!)
  (when (and js/document
             (aget js/document "getElementById"))
    (render [box data] (by-id "app"))))



;; TODO
;; results in columns
