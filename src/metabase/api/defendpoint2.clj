(ns metabase.api.defendpoint2
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [compojure.core :as compojure]
   [malli.core :as mc]
   [malli.transform :as mtx]
   [metabase.api.common.internal :as internal]
   [metabase.config :as config]
   [metabase.util :as u]
   [metabase.util.log :as log]
   [metabase.util.malli.schema :as ms]
   [ring.middleware.multipart-params :as mp]))

;;; almost a copy of malli.experimental.lite, but optional is defined by [:maybe]

(declare make-schema)

(def ^{:dynamic true :private true} *options* nil)
(defn- -entry [[k v]]
  (let [optional (and (vector? v)
                      (= :maybe (first v)))]
    (cond-> [k] optional (conj {:optional true}) :always (conj (make-schema v)))))

(defn make-schema
  "Compile map-based syntax to Malli schema."
  [x]
  (mc/schema (if (map? x)
               (into [:map {:closed config/is-dev?}] (map -entry x))
               x)
             *options*))

;;; endpoint stuff

(s/def ::defendpoint-args
  (s/cat
   :method symbol?
   :route  (some-fn string? sequential?)
   :docstr (s/? string?)
   :args   vector?
   :body   (s/* any?)))

(defn- make-schemas [query-spec]
  (when (map? query-spec)
    (into {} (for [[k v] query-spec
                   :let  [v (dissoc v :as)]]
               (case k
                 :responses [k (update-vals v (fn [v] `(make-schema ~(update-keys v keyword))))]
                 :as        nil
                 [k `(make-schema ~(update-keys v keyword))])))))

(defn- make-bindings [req-sym query-spec]
  (letfn [(walker [m prefix]
            (mapcat (fn [[k v]]
                      (concat
                       (when (symbol? k)
                         [k `(get-in ~req-sym ~(conj prefix (keyword k)))])
                       (when (map? v)
                         (walker v (conj prefix (keyword k))))
                       (when (= k :as)
                         [v `(get-in ~req-sym ~prefix)])))
                    m))]
    (vec (walker query-spec []))))

(comment
  (= (make-bindings 'req '{:query-params {collection [:maybe :whatever]}
                           :body-params  {:some {:inner {value int?}}}})

     `[~'collection (get-in ~'req [:query-params :collection])
       ~'value (get-in ~'req [:body-params :some :inner :value])]))

(def ^:private MTX
  (mtx/transformer
   (mtx/key-transformer {:decode keyword})
   internal/defendpoint-transformer
   (mtx/default-value-transformer)
   mtx/strip-extra-keys-transformer))

(defn make-coercer-mw
  "Middleware"
  [schemas]
  (let [coercers (update-vals schemas #(mc/coercer % MTX))]
    (fn [handler]
      (fn
        ([req]
         (try
           (handler (into req (for [[k coercer] coercers]
                                [k (coercer (get req k))])))
           (catch clojure.lang.ExceptionInfo e
             (let [data (ex-data e)]
               (if (= ::mc/invalid-input (:type data))
                 (throw (ex-info (str "Invalid fields: " (->> data :data :explain :errors (map :in) (map name) (str/join ", ")))
                                 {:status-code     400
                                  :errors          (-> data :data :explain)
                                  :specific-errors (-> data :data :explain)}))
                 (throw e))))))
        ([req respond raise]
         ;; TODO: implement
         (comment req respond raise)
         )))))

(defn- parse-defendpoint-args [args]
  (let [{:keys [method route docstr args body] :as parsed} (s/conform ::defendpoint-args args)]
    (when (= parsed ::s/invalid)
      (throw (ex-info (str "Invalid defendpoint args: " (s/explain-str ::defendpoint-args args))
                      (s/explain-data ::defendpoint-args args))))
    (let [fn-name   (internal/route-fn-name method route)
          schemas   (make-schemas (first args))
          req-sym   (:as (first args) (gensym "req"))
          bindings  (make-bindings req-sym (first args))
          args      (into [req-sym] (rest args))
          #_#_
          docstr    (internal/route-dox method route docstr args
                                        schemas
                                        body)]
      (when-not docstr
        (log/warn (u/format-color 'red "Warning: endpoint %s/%s does not have a docstring. Go add one."
                                  (ns-name *ns*) fn-name)))
      {:method   method
       :route    route
       :fn-name  fn-name
       :docstr   docstr
       :schemas  schemas
       :args     args
       :bindings bindings
       :body     body})))

(defmacro defendpoint2*
  "Impl macro for [[defendpoint2]]; don't use this directly."
  [{:keys [method route fn-name docstr schemas args bindings body]}]
  (let [method-kw       (#'metabase.api.common/method-symbol->keyword method)
        prep-route      #'compojure/prepare-route
        multipart?      (get (meta method) :multipart false)
        handler-wrapper (if multipart? mp/wrap-multipart-params identity)]
    `(let [coercer-mw# (make-coercer-mw ~schemas)]
       (def ~(vary-meta fn-name
                        assoc
                        :doc          docstr
                        :schemas      schemas
                        :is-endpoint? true)
         ;; The next form is a copy of `compojure/compile-route`, with the sole addition of the call to
         ;; `validate-param-values`. This is because to validate the request body we need to intercept the request
         ;; before the destructuring takes place. I.e., we need to validate the value of `(:body request#)`, and that's
         ;; not available if we called `compile-route` ourselves.
         (compojure/make-route
          ~method-kw
          ~(prep-route route)
          (~handler-wrapper
           (coercer-mw#
            (fn ~args
              (let ~bindings
                ~@body)))))))))

(defmacro defendpoint2
  "wut"
  [& args]
  (let [parsed (parse-defendpoint-args args)]
    `(defendpoint2* ~parsed)))

(comment
  (macroexpand '(defendpoint2 POST "/export"
                 [{:query-parameters {collection       [:maybe [:vector ms/PositiveInt]]
                                      all_collections  [:maybe ms/BooleanValue]
                                      settings         [:maybe ms/BooleanValue]
                                      data_model       [:maybe ms/BooleanValue]
                                      field_values     [:maybe ms/BooleanValue]
                                      database_secrets [:maybe ms/BooleanValue]}}]
                  (prn collection settings)))

  (mc/coerce (make-schema
              {:collection       [:maybe [:vector {:decode/string (fn [x] (cond (vector? x) x x [x]))} ms/PositiveInt]]
               :all_collections  [:and {:default true} ms/BooleanValue]
               :settings         [:and {:default true} ms/BooleanValue]
               :data_model       [:and {:default true} ms/BooleanValue]
               :field_values     [:maybe ms/BooleanValue]
               :database_secrets [:maybe ms/BooleanValue]})
             {"settings" "true"}
             MTX)

  (mc/coerce (schema
              {:collection [:maybe [:vector {:decode/string (fn [x] (cond (vector? x) x x [x]))} ms/PositiveInt]]
               :settings   [:and {:default true} ms/BooleanValue]})
             {:collection "1"
              :settings   "false"}
             MTX))
