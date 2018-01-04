package com.wijayaprinting.manager.controller

import com.wijayaprinting.manager.R
import com.wijayaprinting.manager.utils.controller
import com.wijayaprinting.manager.utils.pane
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.stage.Modality.APPLICATION_MODAL
import kotfx.loadFXML
import kotfx.stage
import kotfx.toScene

class PlateController : Controller() {

    @FXML lateinit var priceButton: Button

    @FXML
    fun initialize() {
    }

    @FXML
    fun priceOnAction() {
        val minSize = Pair(240.0, 480.0)
        stage("${getString(R.string.app_name)} - ${getString(R.string.plate_price)}") {
            initModality(APPLICATION_MODAL)
            val loader = getResource(R.fxml.layout_plate_price).loadFXML(resources)
            scene = loader.pane.toScene(minSize.first, minSize.second)
            minWidth = minSize.first
            minHeight = minSize.second
            isResizable = false
            setOnCloseRequest { loader.controller.disposeAll() }
        }.showAndWait()
    }
}