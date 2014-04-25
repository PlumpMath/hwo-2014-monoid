(ns hwo2014bot.trace
  (:require ;[clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [hwo2014bot.protocol :refer :all])
  (:import (java.util Date)))



(defn- new-trace-file [trace-conf]
  {:pre [(not (nil? (:dir trace-conf)))]} ; Preconditions for the function are enforced
  (let [file-path (str (:dir trace-conf) "/trace-" (.getTime (Date.)) "-" (rand-int 1e9) ".log")]
    (log/info "Opening trace file:" file-path)
    (try
      (java.io.FileWriter. file-path)
      (catch java.io.IOException e
        (log/error e "Failed to open trace file:" file-path)
        nil))))

(defn- trace-msg [fh io-mode msg]
  (let [data (if (string? msg) msg (json/write-str msg))
        entry (str "[" (.getTime (Date.)) ", \"" (name io-mode) "\", " data  "]\n")] ; build a json tuple with the timestamp, i/o, msg
    (print-simple entry fh))
  fh)

(defrecord FileTracer [config trace-agent]
  component/Lifecycle
  
  (start [this]
    (send-off trace-agent (fn [_]
                            (new-trace-file config)))
    this)
  
  (stop [this]
    (send-off trace-agent 
              (fn [fh]
                (when fh
                  (.close fh)
                  nil))))
  
  PTrace
  
  (trace [this type-kw msg]
    (send-off trace-agent (fn [fh]
                            (trace-msg fh type-kw msg)))
    this)
    
) ; end record

(defrecord NullTracer []
  component/Lifecycle  
  (start [this] this)
  (stop [this] this)
  
  PTrace
  (trace [this type-kw msg] this)
) ; end record

(defn new-tracer [trace-conf]
  (if trace-conf
    (map->FileTracer
      {:config trace-conf
       ; An agent is an abstraction to queue functions on a thread pool,
       ; with only one function per agent executing at a time.
       :trace-agent (agent nil)})
    (->NullTracer)))
  

