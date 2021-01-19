(ns app.test-helpers
  (:require [cljs.test :as t :include-macros true]
            [cljs.pprint :refer [pprint]]
            [beicon.core :as rx]
            [potok.core :as ptk]
            [app.common.uuid :as uuid]
            [app.common.pages :as cp]
            [app.common.pages.helpers :as cph]))


;; ---- Helpers to manage global events

(defn do-update
  [state event cb]
  (let [new-state (ptk/update event state)]
    (cb new-state)))

(defn do-watch
  [state event cb]
  (->> (ptk/watch event state nil)
       (rx/reduce conj [])
       (rx/subs cb)))

(defn do-watch-update
  [state event & cbs]
  (do-watch state event
    (fn [events]
      (t/is (= (count events) (count cbs)))
      (reduce
        (fn [new-state [event cb]]
          (do-update new-state event cb))
        state
        (map list events cbs)))))


;; ---- Helpers to manage pages and objects

(def initial-state
  {:current-page-id nil
   :workspace-data {:id (uuid/next)
                    :components {}
                    :pages []
                    :pages-index {}}})

(defn current-page
  [state]
  (let [page-id (:current-page-id state)]
    (get-in state [:workspace-data :pages-index page-id])))

(defn sample-page
  ([state] (sample-page state {}))
  ([state {:keys [id name] :as props
           :or {id (uuid/next)
                name "page1"}}]
   (-> state
       (assoc :current-page-id id)
       (update :workspace-data
               cp/process-changes
               [{:type :add-page
                 :id id
                 :name name}]))))

(defn sample-shape
  ([state type] (sample-shape state type {}))
  ([state type props]
   (let [page  (current-page state)
         frame (cph/get-top-frame (:objects page))
         shape (-> (cp/make-minimal-shape type)
                   (merge props))]
     (update state :workspace-data
             cp/process-changes
             [{:type :add-obj
               :id (:id shape)
               :page-id (:id page)
               :frame-id (:id frame)
               :obj shape}]))))

