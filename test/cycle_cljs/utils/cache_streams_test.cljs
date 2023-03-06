(ns test.cycle-cljs.utils.cache-streams-test
  (:require [cljs.test :refer-macros [is deftest async]]
            [cycle-cljs.utils.cache-streams :refer [cache-default-stream]]
            [beicon.core :as $]))

(deftest creates-and-remembers-default-subject-observable
  (async done
         (let [rememberer (cache-default-stream)
               stream1 (rememberer "1")
               stream-same-as-1 (rememberer "1")
               stream2 (rememberer "2")]
           ($/sub! stream1 #(is (= % "stream 1")))
           ($/sub! stream-same-as-1 #(is (= % "stream 1")))
           ($/sub! stream2 #(is (= % "stream 2")))

           (is (= stream1 stream-same-as-1))
           (is not (= stream2 stream-same-as-1))

           ($/push! stream1 "stream 1")
           ($/push! stream2 "stream 2")
           
           ;;  Wait for those observables above to finish
           ($/sub! ($/subject) (done)))))