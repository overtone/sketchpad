(ns sketchpad.repl-communication
  (:use [sketchpad buffer-edit rsyntaxtextarea]
        [seesaw core meta])
  (:require [clojure.string :as string])
  (:import (javax.swing SwingUtilities))
  )

(defn offer! 
  "adds x to the back of queue q"
  [q x] (.offer q x) q)

(defn take! 
  "takes from the front of queue q.  blocks if q is empty"
  [q] (.take q))

(defmacro awtevent [& body]
  `(SwingUtilities/invokeLater
     (fn [] (try ~@body
                 (catch Throwable t# (.printStackTrace t#))))))

(defn- get-classpath []
   (sort (map (memfn getPath) 
              (seq (.getURLs (java.lang.ClassLoader/getSystemClassLoader))))))

    
(defn get-last-cmd [buffer]
  (let [text (config buffer :text)]
   (string/trim (last (string/split text #"=>")))))

(defn clear-repl-input [rsta]
  (let [end (last-visible-offset rsta)
        trim-str (get-last-cmd rsta)
        start (- end (count trim-str))]
    (replace-range! rsta nil start end)))

(defn read-string-at [source-text start-line]
  `(let [sr# (java.io.StringReader. ~source-text)
         rdr# (proxy [clojure.lang.LineNumberingPushbackReader] [sr#]
               (getLineNumber []
                              (+ ~start-line (proxy-super getLineNumber))))]
     (take-while #(not= % :EOF_REACHED)
                 (repeatedly #(try (read rdr#)
                                   (catch Exception e# :EOF_REACHED))))))

(defn replace-first [coll x]
  (cons x (next coll)))

(defn tokens
  "Finds all the tokens in a given string."
  [text]
  (re-seq #"[\w/\.]+" text))

(defn namespaces-from-code
  "Take tokens from text and extract namespace symbols."
  [text]
  (->> text tokens (filter #(.contains % "/"))
       (map #(.split % "/"))
       (map first)
       (map #(when-not (empty? %) (symbol %)))
       (remove nil?)))

(defn cmd-attach-file-and-line [cmd file line]
  (let [read-string-code (read-string-at cmd line)
        short-file (last (.split file "/"))
        namespaces (namespaces-from-code cmd)]
    ;(println namespaces)
    (pr-str
      `(do
         (dorun (map #(try (require %) (catch Exception _#)) '~namespaces))
         (binding [*source-path* ~short-file
                   *file* ~file]
           (last (map eval ~read-string-code)))))))


(defn send-to-project-repl
  ([rsta cmd] (send-to-project-repl rsta cmd "NO_SOURCE_PATH" 0))
  ([rsta cmd file line]
  (awtevent
    (let [cmd-ln (str \newline (.trim cmd) \newline)
          cmd-trim (.trim cmd)]
			;; go to next line
      (append-text-update rsta (str \newline))
      (let [repl (get-meta rsta :repl)
            repl-history (get-meta rsta :repl-history)
            items (repl-history :items)
            cmd-str (cmd-attach-file-and-line (get-last-cmd rsta) file line)]
        (binding [*out* (:input-writer repl)]
          (println cmd-str)
          (flush))
       (when (not= cmd-trim (first @items))
          (swap! items
                 replace-first cmd-trim)
          (swap! items conj ""))
      	(swap! (repl-history :pos) (fn [pos] 0)))))))


(defn send-to-editor-repl
  ([rsta cmd] (send-to-editor-repl rsta cmd "NO_SOURCE_PATH" 0) :repl)
  ([rsta cmd file line] (send-to-editor-repl rsta cmd file line :file))
  ([rsta cmd file line src-key]
   (awtevent   
    (let [repl-history (get-meta rsta :repl-history)
          cmd-ln (str \newline (.trim cmd) \newline)
          cmd-trim (.trim cmd)]
      ;; go to next line
      (append-text-update rsta (str \newline))
      (let [cmd-str (cmd-attach-file-and-line cmd file line)
      			repl-history (get-meta rsta :repl-history)]
        (offer! (get-meta rsta :repl-que) cmd-str))
      (when-let [items (repl-history :items)]
        (when (not= cmd-trim (first @items))
          (swap! items
                 replace-first cmd-trim)
          (swap! items conj ""))
        (swap! (repl-history :pos) (fn [pos] 0)))))))


