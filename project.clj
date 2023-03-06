(defproject cycle-cljs "SNAPSHOT-0.0.1"
  :dependencies [[com.bhauman/figwheel-main "0.2.16"]
                 [com.bhauman/rebel-readline-cljs "0.1.4"]
                 [org.clojure/core.async  "0.4.500"]
                 [cljs-http "0.1.39"]
                 [funcool/beicon "2021.07.05-1"]
                 [cljsjs/snabbdom "3.0.3-0"]]

  :resource-paths ["target" "resources"]

  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]
            "build-dev" ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "-r"]})