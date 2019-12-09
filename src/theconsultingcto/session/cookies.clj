(ns theconsultingcto.session.cookies
  (:require [clojure.string :as s]))

(defn from-headers
  [headers]
  (into {} (map #(s/split % #"=")
                (if (contains? headers "cookie")
                  (s/split (get headers "cookie")
                           #"; ")
                  []))))
