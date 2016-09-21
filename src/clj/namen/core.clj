(ns namen.core 
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found files resources]]
            [compojure.handler :refer [site]]
            [clj-http.client :as client]
            [net.cgrand.enlive-html :as enlive]
            [shoreleave.middleware.rpc :refer [wrap-rpc defremote]]
            [clojure.math.combinatorics :as math]
            [namen.templates.index :refer [index]]
            [cheshire.core :as json]))


(def config {:retry-time 1000})

;;(def google-url "https://www.google.com/search?num=100&safe=off&site=&source=hp&q=")
(def google-url "https://www.google.com/search")

(defn try-n-times [f n]
  (if (zero? n)
    (f)
    (try
      (f)
      (catch Throwable _
        (do (Thread/sleep (:retry-time config))
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


(defn http-get [url opts]
  (let [opts (assoc opts :throw-entire-message? true)]
    (try3 (client/get url opts))))


(defn get-parsed-html
  [url term]
  (-> (http-get url {:headers client-headers
                     :query-params {"num" "100"
                                    "safe" "off"
                                    "source" "hp"
                                    "q" term}})
      :body
      java.io.StringReader.
      enlive/html-resource))


(defn cn-normalize [term]
  (let [url "http://conceptnet5.media.mit.edu/data/5.4/uri"
        res (-> (http-get url {:headers client-headers
                               :query-params {"language" "en"
                                              "text" term}})
                :body
                json/parse-string)]
    (->> (res "uri")
         (re-seq #"\w+$")
         first)))


(defn get-cn-surface [term {:strs [start end surfaceStart]}]
  (let [[start end] (map #(first (re-seq #"\w+$" %)) [start end])]
    (if (= term start)
      end
      (or surfaceStart end))))


(defn cn-lookup [term]
  (let [url "http://conceptnet5.media.mit.edu/data/5.4/c/en/"
        res (-> (str url term)
                (http-get {:headers client-headers
                           :query-params {"limit" 15}})
                :body
                json/parse-string)]
    (map #(get-cn-surface term %) (res "edges"))))



(defn cn-search [term & [rel]]
  (let [relations {:has-property "/r/HasProperty"
                   :capable-of "/r/CapableOf"
                   :used-for "/r/UsedFor"
                   :related-to "/r/RelatedTo"}
        rel (relations (or rel :related-to))
        url "http://conceptnet5.media.mit.edu/data/5.4/search"
        res (-> (http-get url {:headers client-headers
                               :query-params {"limit" 15
                                              "rel" rel
                                              "end" (str "/c/en/" term)}})
                :body
                json/parse-string)]
    (map #(get-cn-surface term %) (res "edges"))))



(defn cn-assoc [term]
  (let [url "http://conceptnet5.media.mit.edu/data/5.4/assoc/list/en/"
        res (-> (str url term)
                (http-get {:headers client-headers
                           :query-params {"limit" 15}})
                :body
                json/parse-string)]
    (map (fn [[c w]]
           (-> (re-seq #"\w+$" c)
               first))
         (res "similar"))))


;; for google
(defn get-dom-text [dom]
  (for [st (enlive/select dom [[:span (enlive/attr= :class "st")]])]
    (enlive/text st)))


;; not working so good
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
       (map #(->> % (interpose " ") (apply str)))))


(defn get-top-word-frequencies [word]
  (->> word
       (get-parsed-html google-url)
       get-dom-text
       (apply str)
       (re-seq #"\w+")
       (remove #(-> % clojure.string/lower-case english-articles))
       frequencies))


(comment google (->> search-terms
                     get-combinations
                     (map #(get-top-word-frequencies %))
                     (reduce #(merge-with + %1 %2))
                     (sort-by val)
                     reverse
                     (take how-many)
                     (map first)
                     (into #{})))

(defremote generate [search-terms how-many]
  (let [cn (let [search-terms (map #(try3 (cn-normalize %)) search-terms)
                 ;; single terms
                 tsks {search-terms
                       [cn-lookup cn-search cn-assoc]}
                 ;; combination of terms
                 tsks (if (< 1 (count search-terms))
                        (assoc tsks
                               (list (apply str (interpose \, search-terms)))
                               [cn-assoc])
                        tsks)]
             (->> (map #(for [t (first %) f (second %)]
                          (f t))
                       tsks)
                  flatten
                  (remove nil?)
                  (map #(clojure.string/replace % "_" " "))
                  distinct))]
    { ;;:google google
     :conceptnet cn}))


;;TODO
;; - search API
;; - again lib
