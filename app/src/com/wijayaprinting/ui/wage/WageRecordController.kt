package com.wijayaprinting.ui.wage

import com.sun.javafx.scene.control.skin.TreeTableViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import com.wijayaprinting.BuildConfig.DEBUG
import com.wijayaprinting.PATTERN_DATE
import com.wijayaprinting.PATTERN_DATETIME
import com.wijayaprinting.PATTERN_TIME
import com.wijayaprinting.R
import com.wijayaprinting.io.ImageFile
import com.wijayaprinting.ui.Controller
import com.wijayaprinting.ui.DateDialog
import com.wijayaprinting.ui.scene.layout.TimeBox
import com.wijayaprinting.util.multithread
import com.wijayaprinting.util.withoutCurrency
import io.reactivex.Completable
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler.platform
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers.io
import javafx.fxml.FXML
import javafx.print.Printer.defaultPrinterProperty
import javafx.scene.control.*
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.stage.Stage
import kotfx.*
import java.awt.print.Printable.NO_SUCH_PAGE
import java.awt.print.Printable.PAGE_EXISTS
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.io.IOException
import java.text.NumberFormat.getCurrencyInstance
import javax.imageio.ImageIO


class WageRecordController : Controller() {

    companion object {
        const val EXTRA_STAGE = "EXTRA_STAGE"
        const val EXTRA_ATTENDEES = "EXTRA_ATTENDEES"
    }

    @FXML lateinit var undoButton: SplitMenuButton
    @FXML lateinit var timeBox: TimeBox
    @FXML lateinit var lockStartButton: Button
    @FXML lateinit var lockEndButton: Button
    @FXML lateinit var printButton: Button

    @FXML lateinit var recordTable: TreeTableView<Record>
    @FXML lateinit var nameColumn: TreeTableColumn<Record, String>
    @FXML lateinit var startColumn: TreeTableColumn<Record, String>
    @FXML lateinit var endColumn: TreeTableColumn<Record, String>
    @FXML lateinit var dailyColumn: TreeTableColumn<Record, Double>
    @FXML lateinit var dailyIncomeColumn: TreeTableColumn<Record, Double>
    @FXML lateinit var overtimeColumn: TreeTableColumn<Record, Double>
    @FXML lateinit var overtimeIncomeColumn: TreeTableColumn<Record, Double>
    @FXML lateinit var totalColumn: TreeTableColumn<Record, Double>

    override fun initialize() {
        undoButton.disableProperty() bind undoButton.items.isEmpty
        arrayOf(lockStartButton, lockEndButton).forEach {
            it.disableProperty() bind (recordTable.selectionModel.selectedItemProperty().isNull or booleanBindingOf(recordTable.selectionModel.selectedItemProperty()) {
                recordTable.selectionModel.selectedItems?.any { !it.value.isChild } ?: true
            })
        }
        printButton.disableProperty() bind defaultPrinterProperty().isNull

        recordTable.selectionModel.selectionMode = MULTIPLE
        recordTable.root = TreeItem(Record)
        recordTable.isShowRoot = false

        nameColumn.setCellValueFactory { it.value.value.displayedName.asProperty().asObservable() }
        startColumn.setCellValueFactory { it.value.value.displayedStart }
        endColumn.setCellValueFactory { it.value.value.displayedEnd }
        dailyColumn.setCellValueFactory { it.value.value.dailyProperty.asObservable() }
        dailyIncomeColumn.setCellValueFactory { it.value.value.dailyIncomeProperty.asObservable() }
        overtimeColumn.setCellValueFactory { it.value.value.overtimeProperty.asObservable() }
        overtimeIncomeColumn.setCellValueFactory { it.value.value.overtimeIncomeProperty.asObservable() }
        totalColumn.setCellValueFactory { it.value.value.totalProperty.asObservable() }

        runFX {
            getExtra<Set<Attendee>>(EXTRA_ATTENDEES).forEach { attendee ->
                val node = attendee.toNodeRecord()
                val childs = attendee.toChildRecords()
                val total = attendee.toTotalRecords(childs)
                recordTable.root.children.add(TreeItem(node).apply {
                    isExpanded = true
                    expandedProperty().addListener { _, _, expanded -> if (!expanded) isExpanded = true } // uncollapsible
                    children.addAll(*childs.map { TreeItem(it) }.toTypedArray(), TreeItem(total))
                })
            }
            getExtra<Stage>(EXTRA_STAGE).titleProperty() bind stringBindingOf(*records.filter { it.isChild }.map { it.totalProperty }.toTypedArray()) {
                getCurrencyInstance().format(records
                        .filter { it.isTotal }
                        .map { it.totalProperty.value }
                        .sum())
                        .let { s -> "${getString(R.string.record)} (${s.withoutCurrency})" }
            }
        }
    }

    @FXML
    fun undo() = undoButton.items[0].fire()

    @FXML
    fun lockStart() {
        val undoable = Undoable()
        recordTable.selectionModel.selectedItems
                .map { it.value }
                .forEach { record ->
                    val initial = record.startProperty.value
                    if (initial.toLocalTime() < timeBox.time) {
                        record.startProperty.set(record.cloneStart(timeBox.time))
                        undoable.name = if (undoable.name == null) "${record.attendee.name} ${initial.toString(PATTERN_DATETIME)} -> ${timeBox.time.toString(PATTERN_TIME)}" else getString(R.string.multiple_lock_start_time)
                        undoable.addAction { record.startProperty.set(initial) }
                    }
                }
        undoable.append()
    }

    @FXML
    fun lockEnd() {
        val undoable = Undoable()
        recordTable.selectionModel.selectedItems
                .map { it.value }
                .forEach { record ->
                    val initial = record.endProperty.value
                    if (initial.toLocalTime() > timeBox.time) {
                        record.endProperty.set(record.cloneEnd(timeBox.time))
                        undoable.name = if (undoable.name == null) "${record.attendee.name} ${initial.toString(PATTERN_DATETIME)} -> ${timeBox.time.toString(PATTERN_TIME)}" else getString(R.string.multiple_lock_end_time)
                        undoable.addAction { record.endProperty.set(initial) }
                    }
                }
        undoable.append()
    }

    @FXML
    fun empty() = DateDialog(this, getString(R.string.empty_daily_income))
            .showAndWait()
            .ifPresent { date ->
                val undoable = Undoable()
                records.filter { it.startProperty.value.toLocalDate() == date }
                        .forEach { record ->
                            val initial = record.dailyEmptyProperty.value
                            record.dailyEmptyProperty.set(!initial)
                            if (undoable.name == null) undoable.name = "${getString(R.string.daily_emptied)} ${record.startProperty.value.toString(PATTERN_DATE)}"
                            undoable.addAction { record.dailyEmptyProperty.set(initial) }
                        }
                undoable.append()
            }

    @FXML
    fun print() = getExternalForm(R.css.style_treetableview_print).let { printStyle ->
        recordTable.stylesheets.add(printStyle)
        val files = mutableListOf<ImageFile>()
        Completable
                .create { emitter ->
                    val flow = (recordTable.skin as TreeTableViewSkin<*>).children[1] as VirtualFlow<*>
                    var i = 0
                    do {
                        val file = ImageFile(i)
                        try {
                            file.write(recordTable.snapshot(null, null))
                            files.add(file)
                        } catch (e: IOException) {
                            emitter.onError(e)
                        }
                        recordTable.scrollTo(flow.lastVisibleCell.index + 2)
                        i++
                    } while (flow.lastVisibleCell.index + 1 < recordTable.root.children.size + recordTable.root.children.map { it.children.size }.sum())
                    emitter.onComplete()
                }
                .multithread(platform(), io())
                .subscribeBy({ e ->
                    recordTable.stylesheets.remove(printStyle)
                    if (DEBUG) e.printStackTrace()
                }, {
                    recordTable.stylesheets.remove(printStyle)
                    val job = PrinterJob.getPrinterJob().apply {
                        setPrintable { graphics, _, pageIndex ->
                            if (pageIndex != 0) NO_SUCH_PAGE
                            val image = ImageIO.read(files[pageIndex])
                            graphics.drawImage(image, 0, 0, image.width, image.height, null)
                            PAGE_EXISTS
                        }
                    }
                    try {
                        job.print()
                    } catch (e: PrinterException) {
                        e.printStackTrace()
                    }
                })
    }

    private val records: List<Record> get() = recordTable.root.children.flatMap { it.children }.map { it.value }

    private fun Undoable.append() {
        if (isValid) undoButton.items.add(0, menuItem {
            text = name
            setOnAction {
                undo()
                undoButton.items.getOrNull(undoButton.items.indexOf(this) - 1)?.fire()
                undoButton.items.remove(this)
            }
        })
    }
}