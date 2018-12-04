package com.hendraanggrian.openpss.ui.invoice

import com.hendraanggrian.openpss.App.Companion.STRETCH_POINT
import com.hendraanggrian.openpss.PATTERN_DATETIME_EXTENDED
import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.control.DateBox
import com.hendraanggrian.openpss.control.IntField
import com.hendraanggrian.openpss.control.PaginatedPane
import com.hendraanggrian.openpss.control.StretchableButton
import com.hendraanggrian.openpss.control.Toolbar
import com.hendraanggrian.openpss.db.SessionWrapper
import com.hendraanggrian.openpss.db.schema.no
import com.hendraanggrian.openpss.db.schemas.Customer
import com.hendraanggrian.openpss.db.schemas.Customers
import com.hendraanggrian.openpss.db.schemas.Employees
import com.hendraanggrian.openpss.db.schemas.Invoice
import com.hendraanggrian.openpss.db.schemas.Invoices
import com.hendraanggrian.openpss.db.schemas.Payment
import com.hendraanggrian.openpss.db.schemas.Payments
import com.hendraanggrian.openpss.db.schemas.Payments.invoiceId
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.popup.dialog.ConfirmDialog
import com.hendraanggrian.openpss.popup.popover.ViewInvoicePopover
import com.hendraanggrian.openpss.ui.ActionController
import com.hendraanggrian.openpss.ui.Refreshable
import com.hendraanggrian.openpss.util.currencyCell
import com.hendraanggrian.openpss.util.doneCell
import com.hendraanggrian.openpss.db.matches
import com.hendraanggrian.openpss.util.stringCell
import javafx.beans.property.SimpleObjectProperty
import javafx.fxml.FXML
import javafx.geometry.Side.BOTTOM
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.RadioButton
import javafx.scene.control.TableView
import javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.util.Callback
import kotlinx.nosql.equal
import kotlinx.nosql.update
import ktfx.application.later
import ktfx.beans.binding.buildBinding
import ktfx.beans.binding.buildStringBinding
import ktfx.beans.value.and
import ktfx.beans.value.eq
import ktfx.beans.value.neq
import ktfx.collections.emptyObservableList
import ktfx.collections.toMutableObservableList
import ktfx.collections.toObservableList
import ktfx.controlsfx.masterDetailPane
import ktfx.coroutines.onAction
import ktfx.coroutines.onMouseClicked
import ktfx.layouts.NodeInvokable
import ktfx.layouts.columns
import ktfx.layouts.contextMenu
import ktfx.layouts.label
import ktfx.layouts.separatorMenuItem
import ktfx.layouts.tableView
import ktfx.scene.control.isSelected
import ktfx.scene.input.isDoubleClick
import org.joda.time.LocalDate
import java.net.URL
import java.util.ResourceBundle
import kotlin.math.ceil

class InvoiceController : ActionController(), Refreshable {

    @FXML lateinit var filterBox: HBox
    @FXML lateinit var allDateRadio: RadioButton
    @FXML lateinit var pickDateRadio: RadioButton
    @FXML lateinit var dateBox: DateBox
    @FXML lateinit var customerField: TextField
    @FXML lateinit var paymentCombo: ComboBox<String>
    @FXML lateinit var invoicePagination: PaginatedPane

    private lateinit var refreshButton: Button
    private lateinit var addButton: Button
    private lateinit var clearFiltersButton: Button
    private lateinit var searchField: IntField

    private val customerProperty = SimpleObjectProperty<Customer>()
    private lateinit var invoiceTable: TableView<Invoice>
    private lateinit var paymentTable: TableView<Payment>

    override fun NodeInvokable.onCreateActions() {
        refreshButton = StretchableButton(
            STRETCH_POINT,
            getString(R.string.refresh),
            ImageView(R.image.act_refresh)
        ).apply {
            onAction { refresh() }
        }()
        addButton = StretchableButton(
            STRETCH_POINT,
            getString(R.string.add),
            ImageView(R.image.act_add)
        ).apply {
            onAction { addInvoice() }
        }()
        clearFiltersButton = StretchableButton(
            STRETCH_POINT,
            getString(R.string.clear_filters),
            ImageView(R.image.act_clear_filters)
        ).apply {
            onAction { clearFilters() }
        }()
        searchField = IntField().apply {
            filterBox.disableProperty().bind(valueProperty() neq 0)
            promptText = getString(R.string.search_no)
        }()
    }

    override fun initialize(location: URL, resources: ResourceBundle) {
        super.initialize(location, resources)
        paymentCombo.run {
            items = listOf(R.string.paid_and_unpaid, R.string.paid, R.string.unpaid)
                .map { getString(it) }
                .toObservableList()
            selectionModel.selectFirst()
        }
        customerField.textProperty().bind(buildStringBinding(customerProperty) {
            customerProperty.value?.toString() ?: getString(R.string.search_customer)
        })
        dateBox.disableProperty().bind(!pickDateRadio.selectedProperty())
        clearFiltersButton.disableProperty().bind(
            pickDateRadio.selectedProperty() and
                (dateBox.valueProperty() eq LocalDate.now()) and
                customerProperty.isNull and
                (paymentCombo.selectionModel.selectedIndexProperty() eq 0)
        )
    }

    override fun refresh() = later {
        invoicePagination.contentFactoryProperty().bind(buildBinding(
            searchField.valueProperty(),
            customerProperty,
            paymentCombo.valueProperty(),
            allDateRadio.selectedProperty(),
            pickDateRadio.selectedProperty(),
            dateBox.valueProperty()
        ) {
            Callback<Pair<Int, Int>, Node> { (page, count) ->
                masterDetailPane(BOTTOM) {
                    invoiceTable = ktfx.layouts.tableView {
                        columnResizePolicy = CONSTRAINED_RESIZE_POLICY
                        columns {
                            getString(R.string.id)<String> { stringCell { no.toString() } }
                            getString(R.string.date)<String> {
                                stringCell { dateTime.toString(PATTERN_DATETIME_EXTENDED) }
                            }
                            getString(R.string.employee)<String> {
                                stringCell { transaction { Employees[employeeId].single().toString() } }
                            }
                            getString(R.string.customer)<String> {
                                stringCell { transaction { Customers[customerId].single().toString() } }
                            }
                            getString(R.string.total)<String> { currencyCell(this@InvoiceController) { total } }
                            getString(R.string.print)<Boolean> { doneCell { printed } }
                            getString(R.string.paid)<Boolean> { doneCell { isPaid } }
                            getString(R.string.done)<Boolean> { doneCell { isDone } }
                        }
                        onMouseClicked {
                            if (it.isDoubleClick() && invoiceTable.selectionModel.isSelected()) {
                                viewInvoice()
                            }
                        }
                        titleProperty().bind(buildStringBinding(selectionModel.selectedItemProperty()) {
                            Invoice.no(this@InvoiceController, selectionModel.selectedItem?.no)
                        })
                    }
                    showDetailNodeProperty().bind(invoiceTable.selectionModel.selectedItemProperty().isNotNull)
                    masterNode = invoiceTable
                    detailNode = ktfx.layouts.vbox {
                        Toolbar().apply {
                            leftItems {
                                label(getString(R.string.payment)) {
                                    styleClass.addAll("bold", "accent")
                                }
                            }
                        }()
                        paymentTable = tableView {
                            columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
                            columns {
                                getString(R.string.date)<String> {
                                    stringCell { dateTime.toString(PATTERN_DATETIME_EXTENDED) }
                                }
                                getString(R.string.employee)<String> {
                                    stringCell { transaction { Employees[employeeId].single().toString() } }
                                }
                                getString(R.string.value)<String> {
                                    currencyCell(this@InvoiceController) { value }
                                }
                                getString(R.string.cash)<Boolean> {
                                    doneCell { isCash() }
                                }
                                getString(R.string.reference)<String> {
                                    stringCell { reference }
                                }
                            }
                            itemsProperty().bind(buildBinding(invoiceTable.selectionModel.selectedItemProperty()) {
                                when (invoiceTable.selectionModel.selectedItem) {
                                    null -> emptyObservableList()
                                    else -> transaction {
                                        Payments {
                                            invoiceId.equal(invoiceTable.selectionModel.selectedItem.id)
                                        }.toObservableList()
                                    }
                                }
                            })
                            contextMenu {
                                getString(R.string.add)(ImageView(R.image.menu_add)) {
                                    disableProperty().bind(invoiceTable.selectionModel.selectedItemProperty().isNull)
                                    onAction { addPayment() }
                                }
                                getString(R.string.delete)(ImageView(R.image.menu_delete)) {
                                    disableProperty().bind(!this@tableView.selectionModel.selectedItemProperty().isNotNull)
                                    onAction { deletePayment() }
                                }
                            }
                        }
                    }
                    dividerPosition = 0.6
                    later {
                        transaction {
                            val invoices = Invoices.buildQuery {
                                when {
                                    searchField.value != 0 -> and(it.no.equal(searchField.value))
                                    else -> {
                                        if (customerProperty.value != null)
                                            and(it.customerId.equal(customerProperty.value.id))
                                        when (paymentCombo.selectionModel.selectedIndex) {
                                            1 -> and(it.isPaid.equal(true))
                                            2 -> and(it.isPaid.equal(false))
                                        }
                                        if (pickDateRadio.isSelected) and(it.dateTime.matches(dateBox.value!!))
                                    }
                                }
                            }
                            invoicePagination.pageCount = ceil(invoices.count() / count.toDouble()).toInt()
                            invoiceTable.items = invoices
                                .skip(count * page)
                                .take(count)
                                .toMutableObservableList()
                            invoiceTable.contextMenu {
                                getString(R.string.view)(ImageView(R.image.menu_invoice)) {
                                    later {
                                        disableProperty().bind(invoiceTable.selectionModel.selectedItemProperty().isNull)
                                    }
                                    onAction { viewInvoice() }
                                }
                                getString(R.string.done)(ImageView(R.image.menu_done)) {
                                    later {
                                        disableProperty().bind(buildBinding(invoiceTable.selectionModel.selectedItemProperty()) {
                                            when {
                                                invoiceTable.selectionModel.selectedItem != null &&
                                                    !invoiceTable.selectionModel.selectedItem.isDone -> false
                                                else -> true
                                            }
                                        })
                                    }
                                    onAction {
                                        transaction {
                                            invoiceTable.selectionModel.selectedItem.done()
                                        }
                                        refreshButton.fire()
                                    }
                                }
                                separatorMenuItem()
                                getString(R.string.delete)(ImageView(R.image.menu_delete)) {
                                    disableProperty().bind(invoiceTable.selectionModel.selectedItemProperty().isNull)
                                    onAction {
                                        (DeleteInvoiceAction(
                                            this@InvoiceController,
                                            invoiceTable.selectionModel.selectedItem
                                        )) {
                                            invoiceTable.items.remove(invoiceTable.selectionModel.selectedItem)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    fun addInvoice() = AddInvoiceDialog(this).show {
        (AddInvoiceAction(this@InvoiceController, it!!)) {
            invoiceTable.items.add(it)
            invoiceTable.selectionModel.selectFirst()
        }
    }

    private fun clearFilters() {
        customerProperty.set(null)
        pickDateRadio.isSelected = true
        dateBox.picker.value = java.time.LocalDate.now()
        paymentCombo.selectionModel.selectFirst()
    }

    @FXML fun selectCustomer() = SearchCustomerPopover(this).show(customerField) { customerProperty.set(it) }

    private fun viewInvoice() = ViewInvoicePopover(this, invoiceTable.selectionModel.selectedItem).apply {
        setOnHidden {
            transaction {
                reload(invoiceTable.selectionModel.selectedItem)
            }
        }
    }.show(invoiceTable)

    private fun addPayment() = AddPaymentPopover(this, invoiceTable.selectionModel.selectedItem).show(paymentTable) {
        (AddPaymentAction(this@InvoiceController, it!!, invoiceTable.selectionModel.selectedItem.no)) {
            updatePaymentStatus()
            reload(invoiceTable.selectionModel.selectedItem)
        }
    }

    private fun deletePayment() = ConfirmDialog(this).show {
        (DeletePaymentAction(
            this@InvoiceController,
            paymentTable.selectionModel.selectedItem,
            invoiceTable.selectionModel.selectedItem.no
        )) {
            updatePaymentStatus()
            reload(invoiceTable.selectionModel.selectedItem)
        }
    }

    private fun SessionWrapper.updatePaymentStatus() = Invoices[invoiceTable.selectionModel.selectedItem]
        .projection { isPaid }
        .update(invoiceTable.selectionModel.selectedItem.calculateDue() <= 0.0)

    private fun SessionWrapper.reload(invoice: Invoice) = invoiceTable.run {
        items.indexOf(invoice).let { index ->
            items[index] = Invoices[invoice].single()
            selectionModel.select(index)
        }
    }
}