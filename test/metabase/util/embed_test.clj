(ns ^:mb/once metabase.util.embed-test
  (:require
   [buddy.sign.jwt :as jwt]
   [crypto.random :as crypto-random]
   [metabase.config :as config]
   [metabase.test :as mt]
   [metabase.util.embed :as embed]))

(def ^:private ^String token-with-alg-none
  "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJhZG1pbiI6dHJ1ZX0.3Dbtd6Z0yuSfw62fOzBGHyiL0BJp3pod_PZE-BBdR-I")

(deftest ^:parallel validate-token-test
  (testing "check that are token is in fact valid"
    (is (= {:admin true}
           (jwt/unsign token-with-alg-none "")))))

(deftest disallow-unsigned-tokens-test
  (testing "check that we disallow tokens signed with alg = none"
    (mt/with-temporary-setting-values [embedding-secret-key (crypto-random/hex 32)]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"JWT `alg` cannot be `none`"
           (embed/unsign token-with-alg-none))))))

(deftest hide-static-embed-terms-test
  (mt/with-test-user :crowberto
    (mt/with-temporary-setting-values [hide-static-embed-terms nil]
      (testing "Check if the user needs to accept the embedding licensing terms before static embedding"
        (when-not config/ee-available?
          (testing "should return false when user is OSS and has not accepted licensing terms"
            (is (= (embed/hide-static-embed-terms) false)))
          (testing "should return true when user is OSS and has already accepted licensing terms"
            (embed/hide-static-embed-terms! true)
            (is (= (embed/hide-static-embed-terms) true))))
        (when config/ee-available?
          (testing "should always return true for EE users"
            (is (= (embed/hide-static-embed-terms) true))
            (embed/hide-static-embed-terms! false)
            (is (= (embed/hide-static-embed-terms) true))
            ))))))
