(ns cycle-cljs.utils.cache-streams
  (:require [beicon.core :as $]))

(defn cache-default-stream
  "Memoizes an observable based on a name. Used for caching streams by their key"
  []
  (let [observables$ (atom {})
        has-name #(contains? (deref observables$) %)
        get #((deref observables$) %)
        put #(swap! observables$ conj {%1 %2})]
    #(if (has-name %) (get %) (do (put % ($/subject)) (get %)))))