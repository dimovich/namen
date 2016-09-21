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
                   :results-visible false}))


(def config {:result-size 30})

;;
;; get results from server
;;
(defn handle-words [words]
  (let [ws (re-seq #"\w+" words)]
    (remote-callback :generate
                     [ws]
                     (fn [res]
                       (->> res
                            (reduce (fn [m [k v]]
                                      (assoc m
                                             k
                                             ;;[[word visible] ...]
                                             (vec (map #(identity [% true]) v)))) 
                                    {})
                            (swap! data assoc-in [:results]))
                       
                       (swap! data assoc :results-visible true)))))



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
   (let [size (js/Math.ceil (/ (count @data) 3))]
     (map-indexed
      (fn [idx1 xs]
        [col {:md 4}
         (map-indexed
          (fn [idx2 [word visible]]
            ^{:key word} [(keyword (str "li.word" (when-not visible ".myhidden")))
                          {:on-click #(swap! data update-in
                                             [(+ idx2 (* size idx1)) 1]
                                             not)}
                          word])
          xs)])
      (partition-all size @data)))])




(defn main-form [state]
  [grid
   [row
    [col {:md 8}
     [page-header ""]]]
   [row
    [col {:md 8}
     [input-form state]]]
   
   [fade {:in (:results-visible @state)}
    [row
     [col {:md 12}
      [row
       [col {:md 8}
        [panel {:header "Thesaurus"
                :collapsible true
                :default-expanded true}
         [word-list (r/cursor state [:results :thesaurus])]]]]
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
;;
;; - CSS columns instead of manually dividing the list
;;
