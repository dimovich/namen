(ns namen.core
  (:require [reagent.core :as r :refer [render]]
            [domina.core :refer [by-id by-class append! destroy! value]]
            [domina.events :refer [listen! prevent-default]]
            [hiccups.runtime]
            [cljs.reader :refer [read-string]]
            [shoreleave.remotes.http-rpc :refer [remote-callback]])
  
  (:require-macros [hiccups.core :refer [html]]
                   [shoreleave.remotes.macros :as macros]))




(defn add-word! [[word weight]]
  (append! (by-class "results")
           (html [:div#word.word word])))


(defn show-words [words]
  (println words)
  (dorun (map add-word! words)))


(defn remove-words! [class]
  (destroy! (by-class class)))


(defn get-words [search-terms]
  (remote-callback :generate
                   [search-terms 33]
                   #(show-words %)))

(defn generate [evt]
  (let [words (value (by-id "words-input"))]
    (when-not (empty? words)
      (do
        (remove-words! "word")
        (->> words
             (re-seq #"\w+")
             get-words)))
    (prevent-default evt)))


(defn ^:export init []
  (enable-console-print!)
  (when (and js/document
             (aget js/document "getElementById"))
    (listen! (by-id "generate-button")
             :click
             generate)))
