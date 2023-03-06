(ns cycle-cljs.core
  (:require [beicon.core :as $]
            [cycle-cljs.run :refer [run]]
            [cycle-cljs.drivers.dom :refer [make-dom-driver]]
            [cycle-cljs.drivers.http :refer [make-http-driver]]
            [snabbdom :refer [h]]))

(defn- pluck-random-image [response]
  (-> (response :body) rand-nth :download_url))

(def image-render (comp
                   #(h "img" (clj->js {:attrs {:src % :width 400} :style {:display "block"}}))
                   #(str % ".jpg")
                   pluck-random-image))

(defn main [{interval :interval
             {http-select :select} :http
             {select-from-dom :select} :dom}]

  (let [initial-vdom (h "div" #js[(h "h1" "Hi from cycle-clojure")
                                  (h "button.click-me" "click me")
                                  (h "button.get-picsum" "get picsum")])]

    {:log ($/merge
           (->> (interval 1000) ($/map #(* 2 %)))
           (->> (http-select "lorem-picsum") ($/map #(str "lorem picsum status code is " (% :status))))
           (->> (select-from-dom ".click-me" {:event "click"}) ($/map (fn [pointer-event]
                                                                        (str "I clicked at {x:" (.-clientX pointer-event) " y:" (.-clientY pointer-event) "}"))))
           (->>
            (select-from-dom ".click-me" {:event "click"})
            ($/map (constantly 1))
            ($/reduce + 0)
            ($/map #(str "We got " %)))

           (->>
            ($/sample-when
             (select-from-dom ".click-me" {:event "click"})
             (http-select "http driver say hello"))
            ($/map #(str "HTTP Driver says: " (% :response))))

           (->>
            (select-from-dom ".now-you-see-me" {:event "click"})
            ($/map (constantly "now you saw me!"))))

     :http ($/merge
            (->>
             ($/of {:category "lorem-picsum"
                    :url "https://picsum.photos/v2/list"})
             ($/delay 5000))
            (->>
             (select-from-dom ".click-me" {:event "click"})
             ($/map (constantly {:category "http driver say hello"})))

            (->>
             (select-from-dom ".get-picsum" {:event "click"})
             ($/map (constantly {:category "lorem-picsum"
                                 :url "https://picsum.photos/v2/list"}))))

     :dom ($/merge
           (->>
            ($/combine-latest
             ($/of initial-vdom)
             ($/merge
              (->> (http-select "lorem-picsum") ($/map image-render))
              ($/of nil))
             ($/merge
              (->> (interval 1500) ($/map #(h "h1" (str "Test! " %))))
              (->> (interval 1100) ($/map #(h "button.now-you-see-me" "Now you see me!")))))
            ($/map #(h "div" %)))

            ;; Initial stream
           ($/of (h "div" initial-vdom)))}))

(defn make-log-driver []
  (fn [log$]
    ;; Subscribe to sink
    ($/sub! log$ #(js/console.log (str "Logging out '" % "'")))

    ;; No source
    {}))

(defn make-interval-driver []
  (fn []
    ;; No sink to subscribe to

    ;; Periodic source
    #($/interval %)))

(run main {:dom (make-dom-driver "#app")
           :log (make-log-driver)
           :interval (make-interval-driver)
           :http (make-http-driver)})