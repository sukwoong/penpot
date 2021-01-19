(ns app.test-library-sync
  (:require [cljs.test :as t :include-macros true]
            [cljs.pprint :refer [pprint]]
            [app.test-helpers :as th]
            [app.common.data :as d]
            [app.common.uuid :as uuid]
            [app.common.pages.helpers :as cph]
            [app.main.data.workspace :as dw]
            [app.main.data.workspace.libraries :as dwl]))

(t/deftest test-add-component
  (t/testing "Add a component"
    (t/async done
      (let [id1   (uuid/next)
            state (-> th/initial-state
                      (th/sample-page)
                      (th/sample-shape :rect {:id id1
                                              :name "Rect 1"}))]
        (th/do-update
          state
          (dw/select-shape id1)
          (fn [new-state]
            (th/do-watch-update
              new-state
              dwl/add-component

              (fn [new-state]
                (let [page  (th/current-page state)
                      shape (cph/get-shape page id1)
                      group (cph/get-shape page (:parent-id shape))]
                  (pprint (:objects page))
                  (t/is (= (:name shape) "Rect 1"))
                  (t/is (= (:name group) "Root Frame"))
                  ))

                  ;; (println "uno")
                  ;; (pprint new-state)))

              (fn [new-state]
                ;; (println "dos")
                ;; (pprint new-state)
                (done)))))))))

(t/deftest test-create-page
  (t/testing "create page"
    (let [state (-> th/initial-state
                    (th/sample-page))
          page  (th/current-page state)]
      (t/is (= (:name page) "page1")))))

(t/deftest test-create-shape
  (t/testing "create shape"
    (let [id    (uuid/next)
          state (-> th/initial-state
                    (th/sample-page)
                    (th/sample-shape :rect {:id id
                                         :name "Rect 1"}))
          page  (th/current-page state)
          shape (cph/get-shape page id)]
      (t/is (= (:name shape) "Rect 1")))))

(t/deftest synctest
  (t/testing "synctest"
    (let [state {:workspace-local {:color-for-rename "something"}}]
      (th/do-update
        state
        dwl/clear-color-for-rename
        (fn [new-state]
          (t/is (= (get-in new-state [:workspace-local :color-for-rename])
                   nil)))))))

(t/deftest asynctest
  (t/testing "asynctest"
    (t/async done
      (let [state {}
            color {:color "#ffffff"}]
        (th/do-watch-update
          state
          (dwl/add-recent-color color)
          (fn [new-state]
            (t/is (= (get-in new-state [:workspace-file
                                        :data
                                        :recent-colors])
                     [color]))
            (t/is (= (get-in new-state [:workspace-data
                                        :recent-colors])
                     [color]))
            (done)))))))

