(ns test.cycle_cljs.run-test
  (:require [cljs.test :refer-macros [is deftest testing async]]
            [cycle-cljs.run :as cycle]
            [beicon.core :as $]
            [test.cycle-cljs.mocks.mock-driver-test :as mock]
            [test.cycle-cljs.mocks.mock-interval-ms :as fast-interval]))

(deftest run-with-simple-sink
  (async done
         (let [spy$ ($/subject) sink$ ($/subject)]
           (cycle/run (constantly {:mock sink$}) {:mock (mock/make-mock-driver spy$)})

           ($/sub! spy$ #(do (is (= % "spy values") "subscribing to mock stream should yield \"spy value\"") (done)))

           ($/push! sink$ "spy values"))))

(deftest run-with-simple-source
  (async done
         (let [spy$ ($/subject)
               obv-spy$ ($/take 5 spy$)
               source$ (fast-interval/interval 1000)
               main (fn [sources] {:mock ($/map #(* % 2) (sources :mock))})
               drivers {:mock (mock/make-mock-driver-with-source spy$ source$)}]
           ($/sub! ($/reduce #(+ %1 %2) obv-spy$) #(do (is (= % 20) "taking 5 from a stream producing numbers from an interval of 1000 should give us 20") (done)))

           (cycle/run main drivers))))

(deftest run-with-dependable-observables
  (async done
         (let [spy$ ($/subject)
               source$ ($/create #($/push! % "observable"))
               drivers {:mock (mock/make-mock-driver spy$)
                        :other-mock (mock/make-mock-driver-with-source ($/subject) source$)}
               main (fn [{other-mock :other-mock}]
                      {:mock ($/map #(reduce str (concat % " <-- from source")) other-mock)})]
           ($/sub! spy$ #(do (is (= % "observable <-- from source")) (done)))

           (cycle/run main drivers))))