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
(def data (r/atom []))

(defn word-component [word weight]
  [:div#word.word word])


(defn word-list [data]
  [:div.results
   (for [[word weight] @data]
     ^{:key word} [word-component word weight])])


(defn handle-words [words]
  (when-not (blank? words)
    (let [ws (re-seq #"\w+" words)]
      (remote-callback :generate
                       [ws 33]
                       #(reset! data %)))))


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
                                              (handle-words @text)))}]
      
       [:button.generate-button {:on-click #(handle-words @text)}
        "Generate"]])))


(def button (r/adapt-react-class (aget js/ReactBootstrap "Button")))

(defn box [data]
  [:div
   [button {:bs-style "success"
            :bs-size "small"
            :on-click #(js/alert "hello")}
    "some button"]
   [word-form]
   [word-list data]])


(defn ^:export init []
  (enable-console-print!)
  (when (and js/document
             (aget js/document "getElementById"))
    (render [box data] (by-id "content"))))
