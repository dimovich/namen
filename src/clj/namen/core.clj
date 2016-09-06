(ns namen.core 
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found files resources]]
            [compojure.handler :refer [site]]
            [clj-http.client :as client]
            [net.cgrand.enlive-html :as enlive]
            [shoreleave.middleware.rpc :refer [wrap-rpc defremote]]
            [clojure.math.combinatorics :as math]
            [namen.templates.index :refer [index]]))

(def google-url "https://www.google.com/search?num=100&safe=off&site=&source=hp&q=")

(def english-articles #{"his" "ago" "of" "up" "off" "theirs" "yours" "mine" "by" "away" "about" "they" "near to" "without" "for" "my" "short" "circa" "a" "on" "notwithstanding" "from" "with" "through" "aside" "your" "to" "hence" "apart" "as" "at" "her" "in" "adjacent to" "on account of" "us" "them" "me" "you" "do" "the" "are" "our" "their" "it" "I" "and" "over" "be" "there" "here" "is" "s" "that" "he" "has" "have" "an" "t" "was"})


(defroutes handler
  (GET "/" [] (index))                  ;; for testing only
  (files "/" {:root "target"})          ;; to serve static resources
  (resources "/" {:root "target"})      ;; to serve anything else
  (not-found "Page Not Found"))         ;; page not found


(def app
  (-> handler
      (wrap-rpc)
      (site)))


(defn get-parsed-html
  [url]
  (-> url
      client/get
      :body
      java.io.StringReader.
      enlive/html-resource))


(defn get-dom-text [dom]
  (for [st (enlive/select dom [[:span (enlive/attr= :class "st")]])]
    (enlive/text st)))


(defn remove-strings [strings words]
  (let [pattern (->> strings
                     (map #(java.util.regex.Pattern/quote %)) 
                     (interpose \|)
                     (apply str))]
    (map #(.replaceAll % pattern "") words)))


(defn get-combinations [words]
  (->> words
       count
       inc
       (range 1)
       (mapcat #(math/combinations words %))
       (map  #(->> % (interpose " ") (apply str)))))


(defn get-top-word-frequencies [word]
  (->> word
       (str google-url)
       get-parsed-html
       get-dom-text
       (apply str)
       (re-seq #"\w+")
       (remove #(-> % clojure.string/lower-case english-articles))
       frequencies))


(defremote generate [search-terms how-many]
  (println "generating...")
  (->> search-terms
       get-combinations
       (map #(get-top-word-frequencies %))
       (reduce #(merge-with + %1 %2))
       (sort-by val)
       reverse
       (take how-many)))


;;TODO
;; - search API
;; - div columns
