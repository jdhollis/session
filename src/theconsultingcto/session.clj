(ns theconsultingcto.session
  (:require [cognitect.aws.client.api :as aws]
            [theconsultingcto.session.csrf-token :as csrf-token]
            [theconsultingcto.session.utilities :as u]
            [nuid.transit :as transit])
  (:import (org.apache.commons.codec.binary Base64)))

(defn- encrypt
  [kms-client key-id plaintext]
  (let [resp (aws/invoke kms-client {:op      :Encrypt
                                     :request {:KeyId     key-id
                                               :Plaintext plaintext}})]
    (Base64/encodeBase64URLSafeString (u/slurp-bytes (:CiphertextBlob resp)))))

(defn- decrypt
  [kms-client key-id ciphertext]
  (let [ciphertext-blob (Base64/decodeBase64 ^String ciphertext)
        resp (aws/invoke kms-client {:op      :Decrypt
                                     :request {:KeyId          key-id
                                               :CiphertextBlob ciphertext-blob}})]
    (slurp (:Plaintext resp))))

(defn from-cookies
  [kms-client key-id cookies]
  (if-let [session-id (get cookies "_session_id")]
    (transit/read (decrypt kms-client key-id session-id))
    {}))

(defn- to-cookie
  [kms-client key-id contents]
  (let [encoded (transit/write contents)
        encrypted (encrypt kms-client key-id encoded)]
    {:Set-Cookie (str "_session_id=" encrypted "; HttpOnly; Path=/; SameSite=None; Secure")}))

(defn with-refreshed-csrf-token
  [kms-client key-id session]
  (let [csrf-token (csrf-token/new)
        masked-csrf-token (csrf-token/masked csrf-token)
        session-cookie (to-cookie kms-client key-id (merge session {:csrf-token csrf-token}))]
    [masked-csrf-token session-cookie]))
