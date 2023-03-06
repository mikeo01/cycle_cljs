(ns cycle-cljs.drivers.dom
  (:require [beicon.core :as $]
            [cljsjs.snabbdom]))

(defn- push-to-subscribers
  "Pushes any argument to subscribers; sugar."
  [subscribers]
  #($/push! subscribers %))

(defn- on-start
  "This function will typically be passed to a fresh producer - essentially it's a cold observable
   as it will start producing it's own events when subscribed to the DOM mutation stream (i.e. when the DOM changes, push out to all subscribers).
   
   It keeps track of any listeners (since there's no native way in JS) so it can attach and deattach as the DOM mutates over time.
   
   Note, there will be N of these at play at any given time depending on the amount of subscribers."
  [dom-mutations$ selector e]
  (let [get-element #(js/document.querySelector %)
        element-exists? #(-> % get-element nil? not)
        add-listener #(if %1 (.addEventListener %1 %2 %3))
        remove-listener #(if %1 (.removeEventListener %1 %2 %3))
        virtual-listeners (atom {})]

    (fn [subscribers]
      (swap! virtual-listeners conj {(str selector e) (push-to-subscribers subscribers)})

      (let [has-event-listener (atom false)
            get-virtual-listener-handler #((deref virtual-listeners) (str %1 %2))]
        ($/sub! dom-mutations$
                #(if (and (element-exists? selector) (false? (deref has-event-listener)))
                   (do
                     (remove-listener (get-element selector) e (get-virtual-listener-handler selector e))
                     (add-listener (get-element selector) e (get-virtual-listener-handler selector e))
                     (reset! has-event-listener true))


                   (if (-> selector element-exists? false?)
                     (do
                       (remove-listener (get-element selector) e (get-virtual-listener-handler selector e))
                       (reset! has-event-listener false)))))))))


(defn- select-from-dom
  "This function takes in a stream of DOM mutations, a DOM selector and an event, and selects / creates a producer that will react to events using addEventListener (documented above)."
  [dom-mutations$
   selector
   {e :event}]

  (let [producer (-> (on-start dom-mutations$ selector e) $/create)]
    ;; Start cold observable producer
    ;; Essentially, we're looking to kickstart this producer when the DOM is ready or a DOM mutation occurs
    ;; This will allow us to attach event listeners and produce streams from them
    ($/sub! producer #())

    producer))

(defn make-dom-driver
  "Takes an inital root DOM selector to mount the application.
   
   This driver initialises Snabbdom and setups the initial observability.
   
   What shall be returned is a function that will listen to all events sinked to this driver that the source will eventually use."
  [selector]
  (let [patch (js/snabbdom.init #js[js/snabbdom.classModule
                                    js/snabbdom.attributesModule
                                    js/snabbdom.datasetModule
                                    js/snabbdom.propsModule
                                    js/snabbdom.styleModule])
        top-level-vnode (js/snabbdom.h (str "div" selector))
        vdom (atom top-level-vnode)
        dom-ready$ ($/create
                    (fn [subs] (.addEventListener js/document "DOMContentLoaded" #($/push! subs nil))))]

    (patch (js/document.querySelector selector) top-level-vnode)
    (reset! vdom top-level-vnode)

    (fn [vdom$]
        ;; Subscribe to Snabbdom streams
      ($/sub! vdom$
              ;; Next
              #(do
                 (patch (deref vdom) %)
                 (reset! vdom %)))

        ;; DOM source
      {:select (partial select-from-dom ($/merge dom-ready$ vdom$))})))

(comment
  ;; Define some view
  (defn view []
    ;; Return Snabbdom's Hyperscript helpers as a stream
    ($/of (js/snabbdom.h "div" #js[(js/snabbdom.h "h1" "Hi there")
                                   (js/snabbdom.h "span" "Rendered by snabbdom")])))

  ;; Define some other views
  (defn view-with-interval []
    ($/merge
     (->>
      ($/interval 1000)
      ($/map #(js/snabbdom.h "h1" (str "Fun with observables - ticking along " %))))
     ($/of (js/snabbdom.h "h1" "Fun with observables"))))

  ;; Initialise DOM driver by attaching to "#app"
  ((make-dom-driver "#app") (view))

  ;; Or, play with observables naturally
  ((make-dom-driver "#app") (view-with-interval)))