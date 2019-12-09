(ns theconsultingcto.session.csrf-token
  (:import (java.security SecureRandom)
           (java.util Arrays)
           (org.apache.commons.codec.binary Base64)))

(def ^:const ^:private token-size 32)

(defn- random-bytes
  []
  (let [bytes (byte-array token-size)]
    (.nextBytes (SecureRandom.) bytes)
    bytes))

(defn new
  []
  (Base64/encodeBase64URLSafeString (random-bytes)))

(defn masked
  [token]
  (let [token-bytes (Base64/decodeBase64 ^String token)
        otp (random-bytes)
        masked-bytes (map bit-xor token-bytes otp)]
    (Base64/encodeBase64URLSafeString (byte-array (concat otp masked-bytes)))))

(defn- unmasked
  [masked]
  (let [masked-bytes (Base64/decodeBase64 ^String masked)
        otp (Arrays/copyOfRange masked-bytes 0 token-size)
        token-bytes (Arrays/copyOfRange masked-bytes token-size (* 2 token-size))
        unmasked-bytes (byte-array (map bit-xor otp token-bytes))]
    (Base64/encodeBase64URLSafeString unmasked-bytes)))

(defn valid?
  [masked original]
  (= original (unmasked masked)))
