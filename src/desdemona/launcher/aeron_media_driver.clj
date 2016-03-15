(ns desdemona.launcher.aeron-media-driver
  (:gen-class)
  (:require
   [clojure.core.async :refer [chan <!!]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as s])
  (:import
   [uk.co.real_logic.aeron.driver MediaDriver MediaDriver$Context]))

(def cli-options
  [["-d" "--delete-dirs"
    "Delete the media drivers directory on startup"
    :default false]
   ["-h" "--help"
    "Display a help message"]])

(def ^:private aeron-launch-error-message
  (str
   "Error starting media driver. This may be due to a media driver data "
   "incompatibility between versions. Check that no other media driver "
   "has been started and then use -d to delete the directory on startup"))

(defn ^:private run-media-driver!
  [options]
  (let [ctx (doto (MediaDriver$Context.)
              (.dirsDeleteOnStart (options :delete-dirs)))]
    (try (MediaDriver/launch ctx)
         (catch IllegalStateException ise
           (throw (Exception. aeron-launch-error-message ise))))))

(defn ^:private run-media-driver-and-block!
  [options]
  (do (run-media-driver! options)
      (println "Launched the Media Driver. Blocking forever...")
      (<!! (chan))))

(defn ^:private usage
  "Given usage summary, returns lines suitable to print as a usage summary."
  [summary]
  (concat ["Usage:" ""] summary))

(defn ^:private cli-error-msg
  "Given errors, returns lines suitable to print as error summary."
  [errors]
  (concat ["Couldn't parse your command:" ""] errors))

(defn ^:private exit!
  "Prints lines to *out* and exit with status."
  [status lines]
  (println (s/join \newline lines))
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      (options :help) (exit! 0 (usage summary))
      errors (exit! 1 (concat (cli-error-msg errors) ( q)))
      :else (run-media-driver-and-block! options))))
