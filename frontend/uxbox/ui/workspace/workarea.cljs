(ns uxbox.ui.workspace.workarea
  (:require [sablono.core :as html :refer-macros [html]]
            [rum.core :as rum]
            [uxbox.router :as r]
            [uxbox.rstore :as rs]
            [uxbox.state :as s]
            [uxbox.data.projects :as dp]
            [uxbox.ui.workspace.base :as wb]
            [uxbox.ui.workspace.rules :as wr]
            [uxbox.ui.mixins :as mx]
            [uxbox.ui.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Coordinates Debug
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn coordenates-render
  []
  (let [[x y] (rum/react wb/mouse-position)]
    (html
     [:div
      {:style {:position "absolute" :left "80px" :top "20px"}}
      [:table
       [:tbody
        [:tr
         [:td "X:"]
         [:td x]]
        [:tr
         [:td "Y:"]
         [:td y]]]]])))

(def coordinates
  (util/component
   {:render coordenates-render
    :name "coordenates"
    :mixins [rum/reactive]}))

(defn background-render
  []
  (html
   [:rect
    {:x 0 :y 0 :width "100%" :height "100%" :fill "white"}]))

(def background
  (util/component
   {:render background-render
    :name "background"
    :mixins [mx/static]}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:static grid-color "#cccccc")

(defn grid-render
  [own enabled? zoom]
  (letfn [(vertical-line [position value padding]
            (let [ticks-mod (/ 100 zoom)
                  step-size (/ 10 zoom)]
              (if (< (mod value ticks-mod) step-size)
                (html [:line {:key position
                              :y1 padding
                              :y2 wb/viewport-width
                              :x1 position
                              :x2 position
                              :stroke grid-color
                              :stroke-width (/ 0.5 zoom)
                              :opacity 0.75}])
                (html [:line {:key position
                              :y1 padding
                              :y2 wb/viewport-width
                              :x1 position
                              :x2 position
                              :stroke grid-color
                              :stroke-width (/ 0.5 zoom)
                              :opacity 0.25}]))))
          (horizontal-line [position value padding]
            (let [ticks-mod (/ 100 zoom)
                  step-size (/ 10 zoom)]
              (if (< (mod value ticks-mod) step-size)
                (html [:line {:key position
                              :y1 position
                              :y2 position
                              :x1 padding
                              :x2 wb/viewport-height
                              :stroke grid-color
                              :stroke-width (/ 0.5 zoom)
                              :opacity 0.75}])
                (html [:line {:key position
                              :y1 position
                              :y2 position
                              :x1 padding
                              :x2 wb/viewport-height
                              :stroke grid-color
                              :stroke-width (/ 0.5 zoom)
                              :opacity 0.25}]))))]
    (let [padding (* 20 zoom)
          ticks-mod (/ 100 zoom)
          step-size (/ 10 zoom)
          vertical-ticks (range (- padding wb/document-start-y)
                                (- wb/viewport-height wb/document-start-y padding)
                                step-size)
          horizontal-ticks (range (- padding wb/document-start-x)
                                  (- wb/viewport-width wb/document-start-x padding)
                                  step-size)]
      (html
       [:g.grid
        {:style {:display (if enabled? "block" "none")}}
        (for [tick vertical-ticks]
          (let [position (+ tick wb/document-start-x)
                line (vertical-line position tick padding)]
            (rum/with-key line (str "tick-" tick))))
        (for [tick horizontal-ticks]
          (let [position (+ tick wb/document-start-y)
                line (horizontal-line position tick padding)]
            (rum/with-key line (str "tick-" tick))))]))))

(def grid
  (util/component
   {:render grid-render
    :name "grid"
    :mixins [mx/static]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canvas
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (rum/defc canvas < rum/reactive
;;                    shapes-push-mixin
;;                    (mx/cmds-mixin
;;                     [::draw draw! (fn [[conn page] shape]
;;                                     (actions/draw-shape conn page shape))]

;;                     [::move move! (fn [[conn] selections]
;;                                     (actions/update-shapes conn selections))])
;;   [conn
;;    page
;;    shapes
;;    {:keys [viewport-height
;;            viewport-width
;;            document-start-x
;;            document-start-y]}]

(defn canvas-render
  []
  (let [page (rum/react wb/page-state)
        page-width (:width page)
        page-height (:height page)
        ;; selection-uuids (rum/react selected-ids)
        ;; selected-shapes (rum/react selected-shapes)
        ;; raw-shapes (into []
        ;;                  (comp
        ;;                   (filter :shape/visible?)
        ;;                   (filter #(not (contains? selection-uuids (:shape/uuid %))))
        ;;                   (map :shape/data))
        ;;                  shapes)
        ]
    (html
     [:svg#page-canvas
      {:x wb/document-start-x
       :y wb/document-start-y
       :ref "canvas"
       :width page-width
       :height page-height
       ;; :on-mouse-down cs/on-mouse-down
       ;; :on-mouse-up cs/on-mouse-up
       }
      (background)
      #_(apply vector :svg#page-layout (map shapes/shape->svg raw-shapes))
      #_(when-let [shape (rum/react drawing)]
          (shapes/shape->drawing-svg shape))
      #_(when-not (empty? selected-shapes)
          (let [rs selected-shapes]
            (vec (cons :g
                       (concat
                        (map shapes/shape->selected-svg rs)
                        (map shapes/shape->svg rs))))))])))

(def canvas
  (util/component
   {:render canvas-render
    :name "canvas"
    :mixins [rum/reactive wb/mouse-mixin]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Viewport
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn viewport-render
  []
  (let [workspace (rum/react wb/workspace-state)
        zoom 1]
    (html
     [:svg#viewport
      {:width wb/viewport-height
       :height wb/viewport-width}
      [:g.zoom
       {:transform (str "scale(" zoom ", " zoom ")")}
       (canvas)
       (grid (:grid-enabled workspace false) zoom)]])))

(def viewport
  (util/component
   {:render viewport-render
    :name "viewport"
    :mixins [rum/reactive]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Aside
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn aside-render
  [own]
  (let [workspace (rum/react wb/workspace-state)]
    [:aside#settings-bar.settings-bar
     [:div.settings-bar-inside
      [:p "foo bar"]
      #_(when (:tools open-setting-boxes)
        (tools open-toolboxes))

      #_(when (:icons open-setting-boxes)
        (icon-sets open-toolboxes))

      #_(when (:components open-setting-boxes)
        (components open-toolboxes components))

      #_(when (:layers open-setting-boxes)
        (layers conn open-toolboxes page shapes))]]))

(def aside
  (util/component
   {:render aside-render
    :name "aside"
    :mixins [rum/reactive]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Work Area
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn working-area-render
  [own]
  (let [workspace (rum/react wb/workspace-state)]
    (html
     [:section.workspace-canvas
      {:class (when (empty? (:toolboxes workspace)) "no-tool-bar")
       :on-scroll (constantly nil)}

      #_(when (:selected page)
          (element-options conn
                           page-cursor
                           project-cursor
                           zoom-cursor
                           shapes-cursor))
      (coordinates)
      (viewport)])))

(def workarea
  (util/component
   {:render working-area-render
    :name "workarea"
    :mixins [rum/reactive]}))

