(ns test.cycle-cljs.mocks.mock-driver-test
  (:require [cljs.test :refer-macros [is deftest testing async]]
            [beicon.core :as $]))

(defn make-mock-driver [spy$]
  (fn [sink$]
    ($/sub! sink$ #($/push! spy$ %))

    ($/empty)))
(defn make-mock-driver-with-source [spy$ source$]
  (fn [sink$]
    ($/sub! sink$ #($/push! spy$ %))

    source$))

;; May seem a bit odd to test a test utility, but it's essentially an actual driver that can be used.
;; in cycle-cljs/run.
;; So it's still bound to the same testing regime 
(deftest mock-driver-test-with-source
  (async done
         (let [spy$ ($/subject)
               sink$ ($/subject)
               source$ ($/subject)
               driver (make-mock-driver-with-source spy$ source$)]
           ($/sub! spy$ #(do (is (= % "some random value")) (done)))

           ($/sub! (driver sink$) (partial $/push! sink$))
           ($/push! source$ "some random value"))))
(deftest mock-driver-test-with-sink-only
  (async done
         (let [spy$ ($/subject)
               sink$ ($/subject)]
           ;; Make driver & call with sink$
           ((make-mock-driver spy$) sink$)
           ($/sub! spy$ #(do (is (= % "some sink value")) (done)))

           ($/push! sink$ "some sink value"))))