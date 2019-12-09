# session

This library provides an encrypted session store using cookies. It also provides a way to generate and validate CSRF tokens.

Both are based on the implementations in [Rails](https://guides.rubyonrails.org/security.html).

The library uses [KMS](https://aws.amazon.com/kms/) to handle encryption and decryption and is intended to be used in conjunction with Cognitect's [aws-api](https://github.com/cognitect-labs/aws-api/).

## Usage

Add these dependencies to your `deps.edn` (or `project.clj`):

```clojure
{:deps {com.cognitect.aws/api        {:mvn/version "0.8.408"}
        com.cognitect.aws/endpoints  {:mvn/version "1.1.11.682"}
        com.cognitect.aws/kms        {:mvn/version "773.2.579.0"}
        com.theconsultingcto/session {:git/url "https://github.com/jdhollis/session.git"
                                      :sha     ""}
        nuid/transit                 {:git/url "https://github.com/nuid/transit.git"
                                      :sha     "cddfa206358c2133f1ebc7090435f73c83130ac3"}}}
```

Strictly speaking, you don't need the `aws-api` dependencies, but I prefer to make transitive dependencies explicit if we're going to use them directly (in this case, we need to create a KMS client).

And `nuid/transit` is only necessary if you're serializing your responses in Transit.  

As for usage in Clojure:

```clojure
(ns namespace                                               ; Insert namespace name here.
  (:require [cognitect.aws.client.api :as aws]
            [theconsultingcto.session :as session]
            [theconsultingcto.session.cookies :as cookies]
            [theconsultingcto.session.csrf-token :as csrf-token]
            [nuid.transit :as transit]))

(def kms
  (delay (aws/client {:api :kms})))                         ; The delay isn't strictly necessary, but it's useful when we want to compile AOT, and we're expecting the runtime environment to provide the necessary AWS credentials.

(defn response
  [status session body]
  (let [[csrf-token session-cookie] (session/with-refreshed-csrf-token @kms kms-session-key-id session) ; kms-session-key-id should be fetched from the runtime environment.
        response {:status  status
                  :headers (merge {:Content-Type "application/transit+json"}
                                  session-cookie)
                  :body    (transit/write (merge body {:csrf-token csrf-token}))}]
    response))

; When responding to a request, you'll want this let somewhere:
(let [masked-csrf-token (get headers "x-csrf-token")
      session (->> (cookies/from-headers headers)
                   (session/from-cookies @kms
                                         kms-session-key-id))]
  (if (and masked-csrf-token
           (csrf-token/valid? masked-csrf-token (:csrf-token session)))
    (response 201 
              session 
              {})
    (response 403
              session
              {:anomaly :invalid-csrf-token})))
```
