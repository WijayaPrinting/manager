package com.hendraanggrian.openpss.ui.wage

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.db.schema.Recesses
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.scene.control.intField
import com.hendraanggrian.openpss.time.FlexibleInterval
import com.hendraanggrian.openpss.time.PATTERN_DATETIME_EXTENDED
import com.hendraanggrian.openpss.ui.DateTimeDialog
import com.hendraanggrian.openpss.ui.Resourced
import com.hendraanggrian.openpss.util.forceRefresh
import com.hendraanggrian.openpss.util.getColor
import com.hendraanggrian.openpss.util.isDelete
import com.hendraanggrian.openpss.util.round
import javafx.geometry.Pos.BOTTOM_CENTER
import javafx.geometry.Pos.TOP_CENTER
import javafx.scene.control.CheckBox
import javafx.scene.control.ContentDisplay.RIGHT
import javafx.scene.control.ListView
import javafx.scene.control.MenuItem
import javafx.scene.control.TitledPane
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.StackPane
import javafx.scene.text.Font
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import ktfx.beans.binding.bindingOf
import ktfx.collections.emptyBinding
import ktfx.collections.sort
import ktfx.coroutines.FX
import ktfx.coroutines.eventFilter
import ktfx.coroutines.listener
import ktfx.coroutines.onAction
import ktfx.coroutines.onKeyPressed
import ktfx.layouts.LayoutDsl
import ktfx.layouts.checkBox
import ktfx.layouts.contextMenu
import ktfx.layouts.gridPane
import ktfx.layouts.imageView
import ktfx.layouts.label
import ktfx.layouts.listView
import ktfx.layouts.menuItem
import ktfx.layouts.separatorMenuItem
import ktfx.layouts.vbox
import ktfx.listeners.cellFactory
import ktfx.scene.layout.gaps
import ktfx.scene.layout.paddings
import ktfx.scene.layout.widthPref
import org.joda.time.DateTime
import kotlin.math.absoluteValue

class AttendeePane(
    resourced: Resourced,
    val attendee: Attendee
) : TitledPane(attendee.toString(), null), Resourced by resourced {

    private lateinit var listView: ListView<DateTime>
    val recessChecks: MutableList<CheckBox> = mutableListOf()
    lateinit var deleteMenu: MenuItem
    lateinit var deleteOthersMenu: MenuItem
    lateinit var deleteToTheRightMenu: MenuItem

    init {
        isCollapsible = false
        content = vbox {
            gridPane {
                gaps = 4
                paddings = 8
                attendee.role?.let { role ->
                    label(getString(R.string.role)) col 0 row 0 marginRight 4
                    label(role) col 1 row 0 colSpan 2
                }
                label(getString(R.string.income)) col 0 row 1 marginRight 4
                intField {
                    widthPref = 88
                    promptText = getString(R.string.income)
                    valueProperty.bindBidirectional(attendee.dailyProperty)
                } col 1 row 1
                label("@${getString(R.string.day)}") { font = Font.font(9.0) } col 2 row 1
                label(getString(R.string.overtime)) col 0 row 2 marginRight 4
                intField {
                    widthPref = 88
                    promptText = getString(R.string.overtime)
                    valueProperty.bindBidirectional(attendee.hourlyOvertimeProperty)
                } col 1 row 2
                label("@${getString(R.string.hour)}") { font = Font.font(9.0) } col 2 row 2
                label(getString(R.string.recess)) col 0 row 3 marginRight 4
                vbox {
                    transaction {
                        Recesses.find().forEach { recess ->
                            recessChecks += checkBox(recess.toString()) {
                                selectedProperty().listener { _, _, selected ->
                                    if (selected) attendee.recesses += recess else attendee.recesses -= recess
                                    listView.forceRefresh()
                                }
                                isSelected = true
                            } marginTop if (children.size > 1) 4 else 0
                        }
                    }
                } col 1 row 3 colSpan 2
            }
            listView = listView(attendee.attendances) {
                widthPref = 128
                cellFactory {
                    onUpdate { dateTime, empty ->
                        clear()
                        if (dateTime != null && !empty) graphic = ktfx.layouts.hbox {
                            val index = listView.items.indexOf(dateTime)
                            alignment = if (index % 2 == 0) BOTTOM_CENTER else TOP_CENTER
                            val itemLabel = label(dateTime.toString(PATTERN_DATETIME_EXTENDED)) {
                                maxWidth = Double.MAX_VALUE
                            } hpriority ALWAYS
                            if (alignment == BOTTOM_CENTER) listView.items.getOrNull(index + 1).let { nextItem ->
                                when (nextItem) {
                                    null -> itemLabel.textFill = getColor(R.color.red)
                                    else -> {
                                        val interval = FlexibleInterval(dateTime, nextItem)
                                        var minutes = interval.minutes
                                        attendee.recesses
                                            .map { it.getInterval(dateTime) }
                                            .forEach {
                                                minutes -= interval.overlap(it)?.toDuration()?.toStandardMinutes()
                                                    ?.minutes?.absoluteValue ?: 0
                                            }
                                        label((minutes / 60.0).round().toString()) { font = Font.font(9.0) }
                                    }
                                }
                            }
                        }
                    }
                }
                onKeyPressed {
                    if (it.code.isDelete() && selectionModel.selectedItem != null)
                        items.remove(selectionModel.selectedItem)
                }
            }
        }
        contextMenu {
            menuItem(getString(R.string.add)) {
                onAction {
                    val prefill = listView.selectionModel.selectedItem ?: DateTime.now()
                    DateTimeDialog(this@AttendeePane, getString(R.string.add_record),
                        prefill.minusMinutes(prefill.minuteOfHour))
                        .showAndWait()
                        .ifPresent {
                            listView.items.add(it)
                            listView.items.sort()
                        }
                }
            }
            menuItem(getString(R.string.edit)) {
                disableProperty().bind(listView.selectionModel.selectedItems.emptyBinding())
                onAction {
                    DateTimeDialog(this@AttendeePane, getString(R.string.edit_record),
                        listView.selectionModel.selectedItem)
                        .showAndWait()
                        .ifPresent {
                            listView.items[listView.selectionModel.selectedIndex] = it
                            listView.items.sort()
                        }
                }
            }
            menuItem(getString(R.string.delete)) {
                disableProperty().bind(listView.selectionModel.selectedItems.emptyBinding())
                onAction { listView.items.remove(listView.selectionModel.selectedItem) }
            }
            separatorMenuItem()
            menuItem(getString(R.string.revert)) { onAction { attendee.attendances.revert() } }
            separatorMenuItem()
            deleteMenu = menuItem("${getString(R.string.delete)} ${attendee.name}")
            deleteOthersMenu = menuItem(getString(R.string.delete_others))
            deleteToTheRightMenu = menuItem(getString(R.string.delete_employees_to_the_right))
        }
        contentDisplay = RIGHT
        graphic = imageView {
            imageProperty().bind(bindingOf(hoverProperty()) {
                Image(when {
                    isHover -> R.image.btn_clear_active
                    else -> R.image.btn_clear_inactive
                })
            })
            eventFilter(type = MOUSE_CLICKED) {
                deleteMenu.fire()
            }
        }
        launch(FX) {
            delay(200)
            applyCss()
            layout()
            val titleRegion = lookup(".title")
            val padding = (titleRegion as StackPane).padding
            val graphicWidth = graphic.layoutBounds.width
            val labelWidth = titleRegion.lookup(".text").layoutBounds.width
            graphicTextGap = width - graphicWidth - padding.left - padding.right - labelWidth
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun attendeePane(
    resourced: Resourced,
    attendee: Attendee,
    noinline init: ((@LayoutDsl AttendeePane).() -> Unit)? = null
): AttendeePane = AttendeePane(resourced, attendee).apply { init?.invoke(this) }