(ns cycle-cljs.run
  (:require [beicon.core :as $]))

(defn make-sink-proxies
  "Given a hash-map of drivers, generate proxy subjects that act as relay observables"
  [drivers]
  (reduce-kv #(assoc %1 %2 ($/subject)) {} drivers))

(defn- make-sources
  "Given a hash-map of sink proxies, pass each sink proxy to it's respective source driver so they each have an observable to subscribe to.
   
   Note, the drivers provided won't be aware that these are not the actual streams produced by the application, hence proxy."
  [drivers sink-proxies]
  (reduce-kv #(assoc %1 %2 (%3 (sink-proxies %2))) {} drivers))

(defn- subscribe-to-sinks
  "Given a hash-map of sink proxies, subscribe to the sinks returned by the main function and proxy pass those values to the sink proxies.
   
   Note here, our sink proxies are actually given to the drivers."
  [sink-proxies sinks]
  (doseq [[sinkName sink] sinks]
    ($/sub! sink
            ;; Next
            #($/push! (sink-proxies sinkName) %)
            ;; Error
            #($/error! (sink-proxies sinkName) %)
            ;; Complete
            #($/end! (sink-proxies sinkName)))))

(defn run
  "Takes a main function which takes a number of sources, runs it to produce observable sinks and connects those sinks up to the provided drivers to create a cyclic observable system."
  [main drivers]
  (let [make-sources (partial make-sources drivers)
        make-sink-proxies (memoize make-sink-proxies)
        subscribe-to-sinks (partial subscribe-to-sinks (make-sink-proxies drivers))]
    (-> drivers
        make-sink-proxies
        make-sources
        main
        subscribe-to-sinks)))

(comment
  (let [main
        (fn [{interval :interval}] {:log (->>
                                          (interval 1000)
                                          ($/map #(* 2 %)))})

        drivers {:log #($/sub! % prn)
                 :interval #(partial $/interval)}]

    (run main drivers)))