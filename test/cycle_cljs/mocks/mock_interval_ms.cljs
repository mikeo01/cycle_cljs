(ns test.cycle-cljs.mocks.mock-interval-ms
  (:require [cljs.test :refer-macros [is deftest testing async]]
            [beicon.core :as $]))

(defn interval [ms] ($/interval (/ 1000 ms)))

(deftest ms-interval
  (async done
         ($/sub!
          (->> (interval 5000) ($/take 5) ($/reduce #(+ %1 %2)))
          #(do (is (= % 10) "+0,1,2,3,4,5 = 10") (done)))))