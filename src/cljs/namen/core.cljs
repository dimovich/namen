(ns namen.core
  (:require [reagent.core :as r :refer [render]]
            [domina.core :refer [value by-id destroy!]]
            [domina.events :refer [listen! prevent-default]]
            [cljs.reader :refer [read-string]]
            [clojure.string :as s :refer [blank?]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]]
            [namen.bootstrap :refer [button navbar navbar-form
                                     form-control form-group form
                                     control-label grid row col
                                     page-header panel input-group
                                     input-group-button fade
                                     loading-button]])
  
  (:require-macros [shoreleave.remotes.macros :as macros]))


;; app state
(def data (r/atom {:results {}
                   :results-visible true}))


(def config {:result-size 30})

;;
;; get results from server
;;
(defn handle-words [words]
  (let [ws (re-seq #"\w+" words)]
    (remote-callback :generate
                     [ws (:result-size config)]
                     #(swap! data assoc-in [:results] %))))



(defn input-form [state]
  (let [text (r/atom "")]
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
                          :state state
                          :state-key :results
                          :on-click #(handle-words @text)}]]]])))



(defn word-list [data]
  [row
   (for [p (partition-all (/ (count @data) 3) @data)]
     [col {:md 4}
      (for [word p]
        ^{:key word} [:div.word {:on-click #(swap! data disj word)}
                      word])])])




(defn main-form [state]
  [grid
   [row
    [col {:md 8}
     [page-header "Ramen Generator"]]]
   [row
    [col {:md 8}
     [input-form state]]]
   
   [fade {:in (:results-visible @state)}
    [row
     [col {:md 12}
      [row
       [col {:md 8}
        [panel {:header "Google"
                :collapsible true
                :default-expanded true}
         [word-list (r/cursor state [:results :google])]]]]
      [row
       [col {:md 8}
        [panel {:header "ConceptNet"
                :collapsible true
                :default-expanded true}
         [word-list (r/cursor state [:results :conceptnet])]]]]]]]])



(defn box [state]
  [:div
   [main-form state]])



(defn ^:export init []
  (enable-console-print!)
  (when (and js/document
             (aget js/document "getElementById"))
    (render [box data] (by-id "app"))))



;; TODO
;; ----
;; - deleted words leave empty space
;;
