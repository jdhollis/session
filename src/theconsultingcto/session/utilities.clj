(ns theconsultingcto.session.utilities
  (:require [clojure.java.io :as io])
  (:import (java.io ByteArrayOutputStream)))

(defn slurp-bytes
  [slurpable]
  (with-open [out (ByteArrayOutputStream.)]
    (io/copy (io/input-stream slurpable) out)
    (.toByteArray out)))
