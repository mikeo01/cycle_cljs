(ns cycle-cljs.drivers.http
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [beicon.core :as $]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! chan]]
            [cycle-cljs.utils.cache-streams :refer [cache-default-stream]]))

(defn- make-request
  "Proxies a request through to cljs-http - note to keep this function simple we do a case match on the :method
   simply to allow for dynamic dispatches.

   Some parameters are defaulted for you. Specifically:
       :with-credentials? false
   
   See the following URL for all request parameters https://github.com/r0man/cljs-http"
  [{url :url
    method :method
    with-credentials? :with-credentials

    :or {method :get
         with-credentials? false}

    :as http-request} callback]

  (let [defaults {:with-credentials? with-credentials?}
        channel (chan)
        http-request (conj http-request defaults {:channel channel})]

    (case method
      :get (http/get url http-request)
      :post (http/post url http-request)
      :put (http/put url http-request)
      :patch (http/patch url http-request)
      :delete (http/delete url http-request))

    (go (-> channel <! callback))))

(defn make-http-driver
  "Subscribes to request streams to perform double dispatching to cljs-http; this blocks until a response is returned which is then pushed out to all subscribers to a particular :category name.
   :category is simply to isolate the XHR streams from each other.
   
   Exposes a source so you can subscribe to streams by providing a :category string / atom"
  []
  (fn [request$]
    (let [create-or-get-stream (cache-default-stream)]

      ;; Subscribe to XHR sink
      ($/sub! request$
                  ;; Next
              (fn [{category :category
                    :as req}]
                (make-request req #($/push! (create-or-get-stream category) %))))

      ;; Source
      {:select #(create-or-get-stream %)})))

(comment
  (defn make-a-request []
    ($/take 100 (->>
                 ($/interval 1)
                 ($/map (constantly {:category "pictures" :url "https://picsum.photos/v2/list"})))))

  (let [http-driver (make-http-driver)
        {select :select} (http-driver (make-a-request))]
    ($/sub! (select "pictures") #(prn "Made a request and got back status " (% :status)))))