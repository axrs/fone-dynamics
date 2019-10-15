(ns io.axrs.cli.fone-dynamics.core
  (:require
    [cli-matic.core :as cli]
    [com.rpl.specter :as sp]
    [io.axrs.cli-tools.env :as env]
    [io.axrs.cli-tools.ansi :as ansi]
    [io.axrs.cli-tools.http :as http]
    [io.axrs.cli-tools.print :as print]
    [io.axrs.cli-tools.time :as time]
    [slingshot.slingshot :refer [try+ throw+]]
    [taoensso.encore :refer [assoc-some]])
  (:gen-class))

(defn- account []
  (env/get "FONE_DYNAMICS_ACCOUNT"))

(defn- token []
  (env/get "FONE_DYNAMICS_TOKEN"))

(defn- property []
  (env/get "FONE_DYNAMICS_PROPERTY"))

(defn- date-range [{:keys [date-from date-to]}]
  (let [to (or (time/->date-time date-to) (time/now))
        from (or (time/->date-time date-from) (time/add-days -7 to))]
    [(-> from time/start-of-day time/->unix)
     (-> to time/end-of-day time/->unix)]))

(defn- get-messages [{:keys [limit] :as params}]
  (try+
    (http/get-json
      (str "https://api.fonedynamics.com/v2/Properties/" (property) "/Messages")
      {:basic-auth [(account) (token)]}
      (assoc-some (zipmap [:Date_From :Date_To] (date-range params))
        :Results_Count limit))
    (catch http/client-error? {:as response}
      (http/print-error response)
      (throw+))))

(defn- seconds->wall-str [seconds]
  (some-> seconds
          time/from-utc-seconds
          time/->wall-str))

(defn- filter-by-params [{:keys [number]} results]
  (if number
    (filter #(or (= number (:to %))
                 (= number (:from %)))
      results)
    results))

(defn- colorize-status [status]
  (let [color-fn (cond
                   (= "Failed" status) ansi/red
                   (= "Delivered" status) ansi/green
                   (= "Processing" status) ansi/blue
                   (= "Received" status) ansi/cyan
                   :else identity)]
    (color-fn status)))

(defn- colourize-number [number v]
  (if (and v (= number v))
    (ansi/green v)
    v))

(defn- colourize [number {:as row}]
  (-> row
      (update :status colorize-status)
      (update :from (partial colourize-number number))
      (update :to (partial colourize-number number))))

(defonce ^:private default-cols
  [:status :created :scheduled :delivered :from :to :text])

(defn- messages [{:keys [number] :as params}]
  (->> (get-messages params)
       :body
       :messages
       (filter-by-params params)
       (sp/transform [sp/ALL (sp/multi-path :created :scheduled :delivered) some?] seconds->wall-str)
       (print/table (partial colourize number) default-cols)))

(defonce ^:private cli-config
  {:app      {:command     "jfd"
              :description "A JESI Encamp Fone Dynamics CLI"
              :version     "0.1"}
   :commands [{:command     "messages"
               :short       "m"
               :description ["Prints a tabular list of messages"]
               :opts        [{:option  "limit"
                              :as      "The total number of messages to fetch before filtering."
                              :type    :int
                              :default 200}
                             {:option "date-from"
                              :as     "Look for messages from a given date (yyyy-mm-dd). Defaults to 7 Days ago"
                              :type   :string}
                             {:option "date-to"
                              :as     "Look for messages to a given date (yyyy-mm-dd). Defaults to today"
                              :type   :string}
                             {:option "number"
                              :as     "Filter messages with a from or to number"
                              :type   :string}]
               :runs        messages}]})

(defn -main [& args]
  (if-not (and (account) (token) (property))
    (print/redln "No FONE_DYNAMICS_ACCOUNT, FONE_DYNAMICS_TOKEN, or FONE_DYNAMICS_PROPERTY defined.")
    (cli/run-cmd args cli-config)))
