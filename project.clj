(defproject overtone-sketchpad "0.0.1-SNAPSHOT"
  :description "A light weight IDE for programming with Overtone and Clojure"
  :main overtone-sketchpad.core
  :dependencies [[org.clojure/clojure "1.3.0"]
  				 [overtone "0.6.0"]
  				 [franks42/seesaw "1.4.2-SNAPSHOT"]
  				 [clooj "0.3.4.2-SNAPSHOT"]
           [com.github.insubstantial/substance "7.1"]]
  :jvm-opts ~(if (= (System/getProperty "os.name") "Mac OS X") ["-Xdock:name=Sketchpad"] [])
  )
