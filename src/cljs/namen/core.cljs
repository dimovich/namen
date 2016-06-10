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

(defn set-data [d]
  (println d)
  (reset! data d))


(defn handle-words-on-click [words]
  (println words)
  (when-not (blank? @words)
    (let [word-list (re-seq #"\w+" @words)]
      (reset! words "")
      (remote-callback :generate
                       [word-list 33]
                       #(reset! data %)
                       ))))


(defn word-form []
  (let [words (r/atom "")]
    (fn []
      [:form#words-form
       [:div
        [:label {:for "words-input"} "Words:"]
        [:input#words-input.words-input {:autofocus "autofocus"
                                         :type "text"
                                         :placeholder "Enter words..."
                                         :value @words
                                         :on-change #(reset! words (-> % .-target .-value))}]]
       
       [:div
        [:input#generate-button.generate-button {:type "button"
                                                 :value "Generate"
                                                 :on-click #(handle-words-on-click words)}]]])))


(defn box [data]
  [:div
   [word-form]
   [word-list data]])


(defn ^:export init []
  (enable-console-print!)
  (when (and js/document
             (aget js/document "getElementById"))
    (render [box data] (by-id "content"))))
