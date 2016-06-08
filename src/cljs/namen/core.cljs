(ns namen.core
  (:require [reagent.core :as r :refer [render]]
            [domina.core :refer [by-id]]
            [domina.events :refer [listen! prevent-default]]
            [cljs.reader :refer [read-string]]
            [clojure.string :as s :refer [blank?]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  
  (:require-macros [shoreleave.remotes.macros :as macros]))


;;word, weight
(def data (r/atom []))


(defn word-component [word weight]
  [:div#word.word word])


(defn word-list [data]
  [:div.results
   (for [[word weight] @data]
     [word-component word weight])])


(defn handle-words-on-click [words]
  (when-not (blank? @words)
    (let [word-list (re-seq #"\w+" @words)]
      (reset! words "")
      (remote-callback :generate
                       [word-list 33]
                       #(reset! data %)))))


(defn word-form []
  (let [words (r/atom "")]
    (fn []
      [:form
       [:div
        [:label {:for "words-input"} "Words:"]
        [:input#words-input.words-input {:autofocus "autofocus"
                                         :type "text"
                                         :placeholder "Enter words..."
                                         :value @words
                                         :on-change #(swap! words assoc (-> % .-target .-value))}]]
       
       [:div
        [:input#generate-button.generate-button {:type "submit"
                                                 :value "Generate"
                                                 :on-click #(handle-words-on-click words)}]]])))


(defn box [data]
  [:div.w-form
   [word-form]
   [word-list data]])


(defn ^:export init []
  (enable-console-print!)
  (when (and js/document
             (aget js/document "getElementById"))
    (render [box data] (by-id "content"))))
