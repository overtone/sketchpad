(ns sketchpad.button-tab
	(:use [seesaw core color border graphics meta]
				[sketchpad option-windows])
	(:import (javax.swing JPanel JLabel BorderFactory AbstractButton JButton)
					 (javax.swing.plaf.basic.BasicButtonUI)
					 (java.awt.event ActionListener MouseListener)
					 (java.awt.BasicStoke)
					 (java.awt Color Dimension Graphics2D FlowLayout)
					 ))

(defn text-area-from-index [app i]
	(select (.getComponentAt (app :editor-tabbed-panel) i) [:#editor]))

(def mouse-over-color (color 200 200 200))
(def base-color (color 150 150 150))
(def pressed-color (color 255 255 255))

(def current-tab-color (atom base-color))


(defn paint-tab-button [c g]
	"custom renderer for tab x"
  (let [;; this gets the parent JTabbedPane via component -> BasicTabbedPaneUI -> JTabbedPane
  			tabbed-pane (.. c getParent getParent getParent) 
  			; current-tab-index (.getSelectedIndex tabbed-pane)
  			; tab-index (@(get-meta c :state) :index)
  			clean? (@(get-meta c :state) :clean)
  			w          (width c)
        h          (height c)
        line-style (style :foreground base-color :stroke 2 :cap :round)
        ellipse-style (style :foreground base-color :background base-color :stroke 2 :cap :round)
        ; line-style (if (= tab-index current-tab-index)
        ; 							(style :foreground @current-tab-color :stroke 2 :cap :round)
        ; 							(style :foreground base-color :stroke 2 :cap :round))
        ; ellipse-style (if (= tab-index current-tab-index)
        ; 							(style :foreground @current-tab-color :background @current-tab-color :stroke 2 :cap :round)
        ; 							(style :foreground base-color :background base-color :stroke 2 :cap :round))
        d 2]
    (cond
     	clean?
	    ;; in clean state draw an X to close the tab
	    (draw g
	      (line d d (- w d) (- h d)) line-style
	      (line d (- h d) (- w d) d) line-style)
    	(not clean?)
    	;; in dirty state draw circle to indicate 
    	(draw g
	      (ellipse d d (- w d d) (- h d d)) ellipse-style)
    	)
    ))

(defn tab-button [tabbed-pane parent-tab app state]
	(let [btn (button :focusable? false
										:tip "close this tab"
										:minimum-size [10 :by 10]
										:size [10 :by 10]
										:id :close-button
										; :border (empty-border :thickness 1)
										:paint paint-tab-button)]
		(doto btn
			(.setBorderPainted false)
			; (.setContentAreaFilled false)
			(.setRolloverEnabled false)
			(.setFocusable false))
		(put-meta! btn :state state)
		btn))

(defn button-tab [app tabbed-pane i]
	(let [rta (text-area-from-index app i)
				state (get-meta rta :state)
				btn-tab (flow-panel :align :right
														; :border (empty-border :thickness 5)
														)
				btn (tab-button tabbed-pane btn-tab app state)
				label (proxy [JLabel] []
								(getText []
									(let [index (.indexOfTabComponent tabbed-pane btn-tab)]
										(if (not= -1 index)
											(.getTitleAt tabbed-pane index)
											nil))))]
		(config! btn-tab :items[label btn])
		(config! label :foreground (color :white)
									 :class [:tab-label])
		;; constructor updates
		(doto btn-tab
			(.setOpaque false)
			(.setBorder (javax.swing.BorderFactory/createEmptyBorder 0 0 0 0))
			(.setMinimumSize (Dimension. 300 20))
			)
		))