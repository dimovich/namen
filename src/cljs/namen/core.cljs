(ns namen.core
  (:require [reagent.core :as r :refer [render]]
            [ajax.core :as ajax :refer [GET]]
            [namen.util :refer [sample]]
            [namen.bootstrap :refer [button navbar navbar-form
                                     form-control form-group form
                                     control-label grid row col
                                     page-header panel input-group
                                     input-group-button fade
                                     loading-button]]))


;; app state
(def app (r/atom {:results {}
                  :results-visible false
                  :less false
                  :white false}))


(def config {:lessize 20})

;;(def by-id (aget js/document "getElementById"))

;;not optimal
(defn seq-to-results [xs]
  (reduce
   (fn [m [k v]]
     (assoc m
            (keyword k)
            ;;[[word visible less] ...]
            (let [s (set (sample (:lessize config) (range (count v))))]
              (vec
               (map-indexed #(identity [%2 true (if (s %1) true false)])
                            v))))) 
   {} xs))

(defn prune-words
  ([xs] (prune-words xs true))
  ([xs invert]
   (reduce
    (fn [m [k v]]
      (assoc m
             k
             (vec (map (fn [[v _ less]]
                         [v 0 less])
                       (remove (fn [[_ selected _]]  (if invert (not selected) selected)) v))))) 
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
                       :auto-focus true
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


(defn word-list [data]
  ;;  @app
  (let [white (:white @app)
        less (:less @app)
        cc 3
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
                       (fn [idx [word visible lss]]
                         (when (or (and lss less) (not less))
                           ^{:key (str (+ idx count) word)}
                           [:div
                            [(keyword (str "span.word" (when-not visible (if white ".mywhite" ".myblack"))))
                             {:on-click #(swap! data update-in
                                                [(+ idx count) 1]
                                                not)}
                             word]]))
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
        google (r/cursor state [:results :google])
        deusu (r/cursor state [:results :deusu])]
    (fn []
      [grid
       #_[row
          [col {:md 8}
           [page-header ""]]]
       [:br]
       [row
        [col {:md 8}
         [input-form state]]]
       #_[row
          [col {:md 2  :md-offset 1}
           [:center
            [:label ;;.switch 
             [:input {:type "checkbox":id "lessmore"
                      :on-click #(swap! state update :less not)}]
             ;;           [:span.slider.round]
             ]
            [:span "  more"]
            ]]

          [col {:md 3}
           [:center
            [:label ;;.switch 
             [:input {:type "checkbox" :id "whiteblack"
                      :on-click #(swap! state update :white not)}]
             ;;[:span.slider.round]
             ]
            [:span "   black"]]]]

       [:br] [:br]
   
       [fade {:in (:results-visible @state)}
        [row
         [col {:md 8}
          [row
           [col {:md 12}
            [panel {:header "ConceptNet"
                    :collapsible true
                    :default-expanded true}
             [word-list conceptnet]]]]

          #_[row
             [col {:md 12}
              [panel {:header "DeuSu"
                      :collapsible true
                      :default-expanded true}
               [word-list deusu]]]]

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
             [word-list thesaurus]]]]]
         
         [col {:md 3 :sm-offset 1 :class "sidebar-outer"}
          [col {:md 6 :class "fixed"}
           [action-menu state]]]]]])))



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
;; - try normal get from cljs-http
;; - save search
;; - print
;;
