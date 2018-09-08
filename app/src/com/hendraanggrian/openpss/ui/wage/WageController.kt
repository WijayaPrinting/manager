package com.hendraanggrian.openpss.ui.wage

import com.hendraanggrian.openpss.App.Companion.STYLE_DEFAULT_BUTTON
import com.hendraanggrian.openpss.BuildConfig.DEBUG
import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.control.stretchableButton
import com.hendraanggrian.openpss.control.styledStretchableButton
import com.hendraanggrian.openpss.io.WageDirectory
import com.hendraanggrian.openpss.io.properties.PreferencesFile.WAGE_READER
import com.hendraanggrian.openpss.layout.SegmentedTabPane.Companion.STRETCH_POINT
import com.hendraanggrian.openpss.ui.SegmentedController
import com.hendraanggrian.openpss.ui.wage.readers.Reader
import com.hendraanggrian.openpss.ui.wage.record.WageRecordController.Companion.EXTRA_ATTENDEES
import com.hendraanggrian.openpss.ui.controller
import com.hendraanggrian.openpss.util.desktop
import com.hendraanggrian.openpss.util.getResource
import com.hendraanggrian.openpss.util.getStyle
import com.hendraanggrian.openpss.ui.pane
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.TitledPane
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.FlowPane
import javafx.stage.FileChooser.ExtensionFilter
import javafxx.application.later
import javafxx.beans.binding.booleanBindingOf
import javafxx.beans.binding.stringBindingOf
import javafxx.beans.value.lessEq
import javafxx.beans.value.or
import javafxx.collections.isEmpty
import javafxx.collections.size
import javafxx.coroutines.FX
import javafxx.coroutines.onAction
import javafxx.layouts.LayoutManager
import javafxx.layouts.borderPane
import javafxx.layouts.label
import javafxx.layouts.styledScene
import javafxx.scene.control.styledErrorAlert
import javafxx.scene.layout.maxSize
import javafxx.stage.fileChooser
import javafxx.stage.setMinSize
import javafxx.stage.stage
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.net.URL
import java.util.ResourceBundle

class WageController : SegmentedController() {

    @FXML lateinit var anchorPane: AnchorPane
    @FXML lateinit var titledPane: TitledPane
    @FXML lateinit var flowPane: FlowPane

    private lateinit var browseButton: Button
    private lateinit var processButton: Button
    private lateinit var disableRecessButton: Button
    private lateinit var recessButton: Button
    private lateinit var historyButton: Button

    override fun LayoutManager<Node>.leftActions() {
        browseButton = stretchableButton(STRETCH_POINT, getString(R.string.browse),
            ImageView(R.image.btn_browse_light)) {
            onAction { browse() }
        }
        space()
        disableRecessButton = stretchableButton(STRETCH_POINT, getString(R.string.disable_recess),
            ImageView(R.image.btn_disable_recess_light)) {
            disableProperty().bind(flowPane.children.isEmpty)
            onAction { disableRecess() }
        }
        processButton = styledStretchableButton(STYLE_DEFAULT_BUTTON, STRETCH_POINT, getString(R.string.process),
            ImageView(R.image.btn_process_dark)) {
            disableProperty().bind(flowPane.children.isEmpty)
            onAction { process() }
        }
    }

    override fun LayoutManager<Node>.rightActions() {
        recessButton = stretchableButton(STRETCH_POINT, getString(R.string.recess),
            ImageView(R.image.btn_recess_light)) {
            onAction { recess() }
        }
        historyButton = stretchableButton(STRETCH_POINT, getString(R.string.history),
            ImageView(R.image.btn_history_light)) {
            onAction { history() }
        }
    }

    override fun initialize(location: URL, resources: ResourceBundle) {
        super.initialize(location, resources)
        titledPane.textProperty().bind(stringBindingOf(flowPane.children) {
            "${flowPane.children.size} ${getString(R.string.employee)}"
        })

        later { flowPane.prefWrapLengthProperty().bind(flowPane.scene.widthProperty()) }
        // if (DEBUG) read(File("/Users/hendraanggrian/Downloads/Absen 4-13-18.xlsx"))
    }

    private fun disableRecess() = DisableRecessPopover(this, attendeePanes).showAt(disableRecessButton)

    private fun process() {
        attendees.forEach { it.saveWage() }
        stage(getString(R.string.record)) {
            val loader = FXMLLoader(getResource(R.layout.controller_wage_record), resources)
            scene = styledScene(getStyle(R.style.openpss), loader.pane)
            setMinSize(1000.0, 650.0)
            loader.controller.addExtra(EXTRA_ATTENDEES, attendees)
        }.showAndWait()
    }

    private fun recess() = EditRecessDialog(this, employee).show()

    private fun history() = desktop?.open(WageDirectory)

    private fun browse() = fileChooser(
        ExtensionFilter(getString(R.string.input_file), *Reader.of(WAGE_READER).extensions))
        .showOpenDialog(anchorPane.scene.window)
        ?.let { read(it) }

    private fun read(file: File) {
        titledPane.graphic = label("${file.absolutePath} -")
        val loadingPane = borderPane {
            prefWidthProperty().bind(titledPane.widthProperty())
            prefHeightProperty().bind(titledPane.heightProperty())
            center = javafxx.layouts.progressIndicator { maxSize = 128.0 }
        }
        anchorPane.children += loadingPane
        flowPane.children.clear()
        launch {
            try {
                Reader.of(WAGE_READER).read(file).forEach { attendee ->
                    attendee.mergeDuplicates()
                    launch(FX) {
                        flowPane.children += attendeePane(this@WageController, attendee) {
                            deleteMenu.onAction {
                                flowPane.children -= this@attendeePane
                                bindProcessButton()
                            }
                            deleteOthersMenu.run {
                                disableProperty().bind(flowPane.children.size() lessEq 1)
                                onAction {
                                    flowPane.children -= flowPane.children.toMutableList().apply {
                                        remove(this@attendeePane)
                                    }
                                    bindProcessButton()
                                }
                            }
                            deleteToTheRightMenu.run {
                                disableProperty().bind(booleanBindingOf(flowPane.children) {
                                    flowPane.children.indexOf(this@attendeePane) == flowPane.children.lastIndex
                                })
                                onAction {
                                    flowPane.children -= flowPane.children.toList().takeLast(
                                        flowPane.children.lastIndex - flowPane.children.indexOf(this@attendeePane))
                                    bindProcessButton()
                                }
                            }
                        }
                    }
                }
                launch(FX) {
                    anchorPane.children -= loadingPane
                    bindProcessButton()
                }
            } catch (e: Exception) {
                if (DEBUG) e.printStackTrace()
                launch(FX) {
                    anchorPane.children -= loadingPane
                    bindProcessButton()
                    styledErrorAlert(getStyle(R.style.openpss), e.message.toString()).show()
                }
            }
        }
    }

    private inline val attendeePanes: List<AttendeePane> get() = flowPane.children.map { (it as AttendeePane) }

    private inline val attendees: List<Attendee> get() = attendeePanes.map { it.attendee }

    /** As attendees are populated, process button need to be rebinded according to new requirements. */
    private fun bindProcessButton() = processButton.disableProperty().bind(flowPane.children.isEmpty or
        booleanBindingOf(flowPane.children, *flowPane.children
            .map { (it as AttendeePane).attendanceList.items }
            .toTypedArray()) { attendees.any { it.attendances.size % 2 != 0 } })
}