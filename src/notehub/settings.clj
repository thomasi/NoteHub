(ns notehub.settings
  (:use [clojure.string
         :rename {replace sreplace}
         :only [blank? trim split split-lines replace upper-case]]))

; Loads and parses any file with each line consisting a key and
; a value separated by a "=", and returns a corresponding key-value map.
(defn- get-pairs-map [file]
  (let [file-content (slurp file)
        pairs (map #(map trim (split % #"=" 2))
                   (remove blank? (split-lines file-content)))]
    (apply hash-map
           (mapcat #(list (keyword (first %)) (second %)) pairs))))

; Loads the setting file to a map
(def settings-map
  (get-pairs-map "settings"))

; Loads the messages file to a map
(def messages-map
  (get-pairs-map "messages"))

(defn get-message [key]
  "Returns messages used in layouts. Every key should be a keyword, e.g. (get-message :title)."
  (messages-map key))

(defn get-setting
  "Takes a settings key, a converter function and a default value, and returns a corresponding
  setting value. The default value is returned back when no setting value was found.
  The converter function can be provided to convert the setting from string to a needed type.
  This function is not applied to the specified default value!
  Every specified key should be a keyword, e.g. (get-setting :page-width)."
  [key & more]
  (let [converter (first more)
        default (second more)
        value (settings-map key)
        ; Through this hack we can read security-critical settings from (previously
        ; set) shell variables without commiting their content to CVS
        value (if value value
                (System/getenv
                 (upper-case
                  (sreplace (name key) #"-" ""))))]
    (if value
      (if (fn? converter) (converter value) value)
      default)))
