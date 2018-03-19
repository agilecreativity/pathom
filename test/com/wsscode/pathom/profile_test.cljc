(ns com.wsscode.pathom.profile-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :refer [go <! #?(:clj <!!)]]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.profile :as pp]
            [com.wsscode.pathom.test :as pt]))

(def make-async-read-plugin
  {::p/wrap-read
   (fn [reader]
     (fn [env]
       (if (-> env :ast :key (pt/hash-mod 2))
         (go (reader env))
         (reader env))))})

(def flame-parser-plugin
  (p/parser {::p/plugins [(p/env-plugin {::p/reader [pt/repeat-reader
                                                     pt/sleep-reader
                                                     pt/self-reader]})
                          pp/profile-plugin]
             :mutate     (fn [_ _ _]
                           {:action
                            (fn []
                              (Thread/sleep 200)
                              {:done true})})}))

(def flame-parser-plugin-async
  (p/async-parser {::p/plugins [(p/env-plugin {::p/reader [pt/repeat-reader
                                                           pt/sleep-reader
                                                           pt/async-reader]})
                                pp/profile-plugin]
                   :mutate     (fn [_ _ _]
                                 {:action
                                  (fn []
                                    (Thread/sleep 200)
                                    {:done true})})}))

(comment
  (->> (flame-parser-plugin {}
         [:hello
          {[:some 300] [:value :path]}
          {:quick [[:slow 1200]
                   {[:deep 400] [[:nest 100]]}]}
          {[:repeat.collection 30] [[:name 3] [:id 20]]}

          ::pp/profile])
       )

  (->> (flame-parser-plugin-async {}
         [:hello
          {[:some 300] [:value :path]}
          {:quick [[:slow 1200]
                   {[:deep 400] [[:nest 100]]}]}
          {[:repeat.collection 30] [[:name 3] [:id 20]]}

          ::pp/profile])
       <!!)

  (->> (flame-parser-plugin {} ['(call-me/maybe {:a 1})
                                ::pp/profile]))

  (-> flame-sample ::pp/profile
      (pp/profile->nvc)
      #_(clojure.data.json/write-str)
      #_println))
