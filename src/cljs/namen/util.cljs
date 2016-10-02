(ns namen.util)

;;Dirty Hair


;; Knuth shuffle
(defn sample
  ([n xs] (sample n xs (count xs) '()))
  ([n xs len res]
   (cond (= len 0) res
         (< (* len (rand 1)) n) (sample (dec n)
                                        (rest xs)
                                        (dec len)
                                        (cons (first xs) res))
         :else (sample n
                       (rest xs)
                       (dec len)
                       res))))
