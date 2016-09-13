(ns namen.core 
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found files resources]]
            [compojure.handler :refer [site]]
            [clj-http.client :as client]
            [net.cgrand.enlive-html :as enlive]
            [shoreleave.middleware.rpc :refer [wrap-rpc defremote]]
            [clojure.math.combinatorics :as math]
            [namen.templates.index :refer [index]]))


(def app (atom {:retry-time 1000}))

;;(def google-url "https://www.google.com/search?num=100&safe=off&site=&source=hp&q=")
(def google-url "https://www.google.com/search")

(defn try-n-times [f n]
  (if (zero? n)
    (f)
    (try
      (println "[trying " n "]")
      (f)
      (catch Throwable _
        (do (Thread/sleep (:retry-time @app))
            (try-n-times f (dec n)))))))

(defmacro try3 [& body]
  `(try-n-times (fn [] ~@body) 10))




(def client-headers {"User-Agent" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:48.0) Gecko/20100101 Firefox/48.0"})

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
  [url term]
  (-> (try3 (client/get url
                        {:headers client-headers
                         :throw-entire-message? true
                         :query-params {"num" "100"
                                        "safe" "off"
                                        "source" "hp"
                                        "q" term}}))
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
       (get-parsed-html google-url)
       get-dom-text
       (apply str)
       (re-seq #"\w+")
       (remove #(-> % clojure.string/lower-case english-articles))
       frequencies))


(defremote generate [search-terms how-many]
  (->> search-terms
       get-combinations
       (map #(get-top-word-frequencies %))
       (reduce #(merge-with + %1 %2))
       (sort-by val)
       reverse
       (take how-many)
       (into #{})))


;;TODO
;; - search API
;; - conceptNet API
;; - again lib
