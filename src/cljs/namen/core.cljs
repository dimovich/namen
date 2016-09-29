(ns namen.core
  (:require [reagent.core :as r :refer [render]]
            [ajax.core :as ajax :refer [GET]]
            [namen.bootstrap :refer [button navbar navbar-form
                                     form-control form-group form
                                     control-label grid row col
                                     page-header panel input-group
                                     input-group-button fade
                                     loading-button]]))


;; app state
(def app (r/atom {:results {}
                  :results-visible false
                  :less true
                  :white true}))


(def config {:lessize 30})

;;(def by-id (aget js/document "getElementById"))

;;not optimal
(defn seq-to-results [xs]
  (reduce
   (fn [m [k v]]
     (assoc m
            (keyword k)
            ;;[[word visible] ...]
            (vec (map #(identity [% true])
                      (if (:less @app)
                        (take (:lessize config) v)
                        v))))) 
   {} xs))

(defn prune-words
  ([xs] (prune-words xs true))
  ([xs invert]
   (reduce
    (fn [m [k v]]
      (assoc m
             k
             (vec (remove (fn [[_ visible]]  (if invert (not visible) visible)) v)))) 
    {} xs)))

;;
;; get results from server
;;
(defn handle-words [words]
  (let [words (re-seq #"\w+" words)]
    (GET "/generate" {:handler (fn [res]
                                 (->> res
                                      seq-to-results
                                      (swap! app assoc-in [:results]))
                                 (swap! app assoc :results-visible true))
                      
                      :error-handler #(.log js/console "error getting results...")
                      :params {:words words}
                      :response-format (ajax/json-response-format)})))



(defn input-form [state]
  (let [text (r/atom "")]
    (fn []
      [form-group
       [input-group
        [form-control {:type "text"
                       :placeholder "insert keywords"
                       :value @text
                       :on-change #(reset! text (-> % .-target .-value))
                       :on-key-press (fn [e]
                                       (when (= 13 (.-charCode e))
                                         (.click (.getElementById js/document "generate"))))}]
        [input-group-button
         [loading-button {:etext "Generate"
                          :dtext "Generating..."
                          :opts {:id "generate"}
                          :state state
                          :state-key :results
                          :on-click #(handle-words @text)}]]]])))


(defn word-list [data white]
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
                          [(keyword (str "span.word" (when-not visible (if white ".mywhite" ".myblack"))))
                           {:on-click #(swap! data update-in
                                              [(+ idx count) 1]
                                              not)}
                           word]])
                       (take batch xs))]))))))


(defn action-menu [state]
  [:div
   [:p [:span {:class "action"
               :on-click #(swap! state update-in [:results] prune-words (:white @state))}
        (str (if (:white @state) "delete" "mark") " words")]]
   [:p [:span {:class "action"
               :on-click (fn [] (do
                                  (swap! state update-in [:results] #(identity {}))
                                  (swap! state assoc :results-visible false)))}
        "reset"]]])



(defn main-form [state]
  (let [thesaurus (r/cursor state [:results :thesaurus])
        conceptnet (r/cursor state [:results :conceptnet])
        ;; google (r/cursor state [:results :google])
        white (r/cursor state [:white])]
    (fn []
      [grid
       [row
        [col {:md 8}
         [page-header ""]]]
       [row
        [col {:md 8}
         [input-form state]]]
       [row
        [col {:md 3;; :sm-offset 1
              }
         [:center
          [:span "less "]
          [:label.switch 
           [:input {:type "checkbox":id "lessmore"
                    :on-click #(swap! state update :less not)}]
           [:span.slider.round]]
          [:span "  more"]]]

        [col {:md 3}
         [:center
          [:span "white"]
          [:label.switch 
           [:input {:type "checkbox" :id "whiteblack"
                    :on-click #(swap! state update :white not)}]
           [:span.slider.round]]
          [:span "   black"]]]]

       [:p] [:p]
   
       [fade {:in (:results-visible @state)}
        [row
         [col {:md 8}
          [row
           [col {:md 12}
            [panel {:header "ConceptNet"
                    :collapsible true
                    :default-expanded true}
             [word-list conceptnet white]]]]

          #_[row
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
             [word-list thesaurus white]]]]]
         
         [col {:md 3 :sm-offset 1 :class "sidebar-outer"}
          [col {:md 3 :class "fixed"}
           [action-menu state]]]
         ]]])))



(defn box [state]
  [:div
   [main-form state]])



(defn ^:export init []
  ;;  (enable-console-print!)
  (when js/document
    (render [box app] (.getElementById js/document "app"))))



;; TODO
;; ----
;;
;; - CSS columns instead of manually dividing the list
;; - recover from blocked remote procedure
;;
