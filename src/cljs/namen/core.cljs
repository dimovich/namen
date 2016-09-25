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
(def app (r/atom {:results {}
                  :results-visible false
                  :less true}))


(def config {:lessize 30})


;;not optimal
(defn seq-to-results [xs]
  (reduce
   (fn [m [k v]]
     (assoc m
            k
            ;;[[word visible] ...]
            (vec (map #(identity [% true])
                      (if (:less @app)
                        (take (:lessize config) v)
                        v))))) 
   {} xs))

(defn prune-words [xs]
  (reduce
   (fn [m [k v]]
     (assoc m
            k
            (vec (remove (fn [[_ visible]]  (not visible)) v)))) 
   {} xs))

;;
;; get results from server
;;
(defn handle-words [words]
  (let [ws (re-seq #"\w+" words)]
    (remote-callback :generate
                     [ws]
                     (fn [res]
                       (->> res
                            seq-to-results
                            (swap! app assoc-in [:results]))
                       
                       (swap! app assoc :results-visible true)))))



(defn input-form [state]
  (let [text (r/atom "")]
    (fn []
      [form-group
       [input-group
        [form-control {:type "text"
                       :placeholder "insert keywords"
                       :value @text
                       :on-change #(reset! text (-> % .-target .-value))
;;                       :on-submit #(prevent-default %) ;; do we need this?
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
  (let [cc 3
        batch (js/Math.ceil (/ (count @data) cc))
        md (/ 12 cc)]
    (loop [count 0
           xs @data
           content [row]]
      (if (empty? xs)
        content
        (recur (+ count batch) (drop batch xs)
               (conj content
                     [col {:md md}
                      (map-indexed
                       (fn [idx [word visible]]
                         ^{:key (str (+ idx count) word)}
                         [:div
                          [(keyword (str "span.word" (when-not visible ".myhidden")))
                           {:on-click #(swap! data update-in
                                              [(+ idx count) 1]
                                              not)}
                           word]])
                       (take batch xs))]))))))


(defn action-menu [state]
  [:div
   [:p [:span {:class "action"
               :on-click #(swap! state update-in [:results] prune-words)}
        "delete words"]]
   [:p [:span {:class "action"
               :on-click (fn [] (do
                                  (swap! state update-in [:results] #(identity {}))
                                  (swap! state assoc :results-visible false)))}
        "reset"]]])



(defn main-form [state]
  (let [thesaurus (r/cursor state [:results :thesaurus])
        conceptnet (r/cursor state [:results :conceptnet])
        google (r/cursor state [:results :google])]
    (fn []
      [grid
       [row
        [col {:md 8}
         [page-header ""]]]
       [row
        [col {:md 8}
         [input-form state]]]
       [row
        [col {:md 8}
         [:center
          [:span "less "]
          [:label.switch 
           [:input {:type "checkbox":id "lessmore"
                    :on-click #(swap! state update :less not)}]
           [:span.slider.round]]
          [:span "  more"]]]]

       [:p] [:p]
   
       [fade {:in (:results-visible @state)}
        [row
         [col {:md 8}
          [row
           [col {:md 12}
            [panel {:header "ConceptNet"
                    :collapsible true
                    :default-expanded true}
             [word-list conceptnet]]]]

          [row
           [col {:md 12}
            [panel {:header "Google"
                    :collapsible true
                    :default-expanded true}
             [word-list google]]]]

          [row
           [col {:md 12}
            [panel {:header "Thesaurus"
                    :collapsible true
                    :default-expanded true}
             [word-list thesaurus]]]]]
         
         [col {:md 3 :sm-offset 1 :class "sidebar-outer"}
          [col {:md 3 :class "fixed"}
           [action-menu state]]]
         ]]])))



(defn box [state]
  [:div
   [main-form state]])



(defn ^:export init []
  (enable-console-print!)
  (when (and js/document
             (aget js/document "getElementById"))
    (render [box app] (by-id "app"))))



;; TODO
;; ----
;;
;; - CSS columns instead of manually dividing the list
;; - recover from blocked remote procedure
;;
