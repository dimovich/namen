(ns namen.core 
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :refer [not-found files resources]]
            [compojure.handler :refer [site]]
            [clj-http.client :as client]
            [net.cgrand.enlive-html :as enlive]
            [clojure.math.combinatorics :as math]
            [namen.templates.index :refer [index]]
            [cheshire.core :as json]
            [slingshot.slingshot :refer [try+ throw+]]))


(def config {:retry-time 1000
             :google-size 50})


(def google-url "https://www.google.com/search")
(def ts-url "http://www.thesaurus.com/browse/")


;; for urls
(defn try-n-times [f n]
  (if (zero? n)
    ;;(f)
    ""
    (try+
     (f)
     (catch [:status 403] _;;{:keys [request-time headers body]}
       ;;(println "403" request-time headers)
       "")
     (catch [:status 404] _;;{:keys [request-time headers body]}
       ;;(println "NOT Found 404" request-time headers body)
       "")
     (catch Object _
       (do
         ;;(println (:throwable &throw-context) "unexpected error")
         (println "unexpected error")
         (Thread/sleep (:retry-time config))
         (try-n-times f (dec n)))))))

(defmacro try3 [& body]
  `(try-n-times (fn [] ~@body) 10))




(def client-headers {"User-Agent" "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:48.0) Gecko/20100101 Firefox/48.0"})

(def english-articles #{"his" "ago" "of" "up" "off" "theirs" "yours" "mine" "by" "away" "about" "they" "near to" "without" "for" "my" "short" "circa" "a" "on" "notwithstanding" "from" "with" "through" "aside" "your" "to" "hence" "apart" "as" "at" "her" "in" "adjacent to" "on account of" "us" "them" "me" "you" "do" "the" "are" "our" "their" "it" "I" "and" "over" "be" "there" "here" "is" "s" "that" "he" "has" "have" "an" "t" "was" "all" "its" "two" "three" "into" "than" "more" "if" "also" "or" "when" "then" "each" "across" "out" "where" "can" "one" "this" "not" "these" "non" "most" "will" "why" "may" "how"})





(defn http-get [url opts]
  (let [opts (assoc opts :throw-entire-message? true :headers client-headers)]
    (try3 (client/get url opts))))



(defn parse-html
  [html]
  (when-not (empty? html)
    (-> html
        java.io.StringReader.
        enlive/html-resource)))


(defn get-html
  ([url]
   (get-html url {}))
  ([url opts]
   (-> (http-get url opts)
       :body)))


(defn extract-text [dom selectors]
  (for [st (enlive/select dom [selectors])]
    (enlive/text st)))


;;
;; Thesaurus
;;
(defn ts-search [term]
  (let [st [[:span (enlive/attr= :class "text")]
            [:a (enlive/attr= :class "syn_of_syns")]]
        dom (-> (str ts-url term)
                get-html
                parse-html)]
    (mapcat #(extract-text dom %) st)))



(defn cn-normalize [term]
  (let [url "http://conceptnet5.media.mit.edu/data/5.4/uri"
        res (-> (http-get url {:headers client-headers
                               :query-params {"language" "en"
                                              "text" term}})
                :body
                json/parse-string)]
    ;;(println "===!!!!!!!!!!! " res " !!!!!!!!111======")
    (if res
      (->> (res "uri")
           (re-seq #"\w+$")
           first)
      "")))


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
                json/parse-string)
        res (or res ())]
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


(defn get-combinations [words]
  (->> words
       count
       inc
       (range 1)
       (mapcat #(math/combinations words %))
       (map #(->> % (interpose " ") (apply str)))))


(defn google-search [term]
  ;;find words next to our term
  (let [p (re-pattern (str "(\\w+)?\\s*" term "\\s*(\\w+)?"))]
    (->>
     (-> (get-html google-url {:query-params {"num" "100"
                                              "safe" "off"
                                              "source" "hp"
                                              "q" term}})
         (parse-html)
         (extract-text [[:span (enlive/attr= :class "st")]]))
     (apply str)
     clojure.string/lower-case
     ;;deconstruct and clean
     (re-seq #"\b[^\d\W]{3,}\b")
     (remove english-articles)
     ;;reconstruct and analyze
     (interpose " ")
     (apply str)
     (re-seq p)
     (mapcat rest)
     (remove nil?)
     (remove #{"s"})
     frequencies)))


(defn generate [search-terms]
  (println "generating: " search-terms)
  (let [ ;; ConceptNet
        cn (let [search-terms (map #(cn-normalize %) search-terms)
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
                  distinct))
        ;; Thesaurus
        ts (mapcat #(ts-search %) search-terms)

        ;; Google
        google (->> search-terms
                    get-combinations
                    (map #(google-search %))
                    (reduce #(merge-with + %1 %2))
                    (sort-by val)
                    reverse
                    (take (:google-size config))
                    (map first)
                    ;;distinct
                    )]
    {:google google
     :conceptnet cn
     :thesaurus ts}))



(defroutes handler
  (GET "/" [] (index)) ;; for testing only
  (GET "/generate" xs (json/generate-string
                       (generate (-> xs :params :words vals))))
  (files "/" {:root "target"})     ;; to serve static resources
  (resources "/" {:root "target"}) ;; to serve anything else
  (not-found "Page Not Found"))    ;; page not found


(def app
  (-> handler
      (site)))



;;TODO
;; - wiktionary
;; - again lib
;; - deal with those network errors better...



#_(defn wrap-unknown-host
  "Middleware ignoring unknown hosts when the :ignore-unknown-host? option
  is set."
  [client]
  (fn [req]
    (try
      (client req)
      (catch Exception e
        (if (= (type (root-cause e)) UnknownHostException)
          (when-not (opt req :ignore-unknown-host)
            (throw (root-cause e)))
          (throw (root-cause e)))))))
