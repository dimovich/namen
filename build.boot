(set-env!
 :source-paths #{"src/clj" "src/cljs"}
 :resource-paths #{"resources" "src/clj"}

 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]

                 [adzerk/boot-cljs          "2.0.0"      :scope "test"]
                 [adzerk/boot-cljs-repl     "0.3.2"      :scope "test"]
                 [adzerk/boot-reload        "0.5.1"      :scope "test"]
                 [pandeiro/boot-http        "0.8.3"      :scope "test"]
                 [com.cemerick/piggieback   "0.2.1"      :scope "test"]
                 [org.clojure/tools.nrepl   "0.2.13"     :scope "test"]
                 [weasel                    "0.7.0"      :scope "test"]
                 [tolitius/boot-check       "0.1.4"      :scope "test"]

                 [compojure "1.5.1"]
                 [hiccup    "2.0.0-alpha1"]
                 [javax.servlet/servlet-api "3.0-alpha-1"]
                 [enlive "1.1.6"]
                 [reagent "0.6.0"]
                 [clj-http "2.2.0"]
                 [org.clojure/math.combinatorics "0.1.3"]
                 [cljsjs/react-bootstrap "0.30.2-0"]
                 [cheshire "5.6.3"]
                 [cljs-ajax "0.5.8"]
                 [slingshot "0.12.2"]])


(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         'boot.repl)



(swap! boot.repl/*default-dependencies*
       concat '[[cider/cider-nrepl "0.15.0-SNAPSHOT"]])

(swap! boot.repl/*default-middleware*
       conj 'cider.nrepl/cider-middleware)


(task-options!
 pom {:project 'namen
      :version "0.1.0"}
 jar  {:file "namen.jar"}
 sift {:include #{#"namen.jar" #"assets" #"namen.js"}})


(deftask production
  []
  (task-options! cljs {:optimizations :advanced}
                 target {:dir #{"release"}})
  identity)


(deftask development
  []
  (task-options! cljs      {:optimizations :none
                            :source-map    true}
                 cljs-repl {:nrepl-opts {:port 3311}}
                 target {:dir #{"target"}})
  identity)



(deftask build-jar
  []
  (comp (aot  :namespace #{'namen.core})
        (uber)
        (jar)
        (sift)))


(deftask run []
  (comp
   (serve :resource-root "target/public"
          :handler 'namen.core/app
          :reload true
          :httpkit true)
   (watch)
   (reload)
   (cljs-repl)
   (cljs)
   (target)))



(deftask dev
  []
  ;;(task-options! reload {:on-jsload 'namen.core/reload})
  (comp (development)
        (run)))


(deftask prod
  []
  (comp (production)
        (cljs)
        (build-jar)
        (target)))
