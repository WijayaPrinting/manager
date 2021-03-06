package com.hendraanggrian.openpss.ui.invoice

import com.hendraanggrian.openpss.PATTERN_DATETIMEEXT
import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.R2
import com.hendraanggrian.openpss.api.OpenPSSApi
import com.hendraanggrian.openpss.control.Action
import com.hendraanggrian.openpss.control.DateBox
import com.hendraanggrian.openpss.control.IntField
import com.hendraanggrian.openpss.control.PaginatedPane
import com.hendraanggrian.openpss.control.Toolbar
import com.hendraanggrian.openpss.schema.Customer
import com.hendraanggrian.openpss.schema.Invoice
import com.hendraanggrian.openpss.schema.Payment
import com.hendraanggrian.openpss.schema.no
import com.hendraanggrian.openpss.ui.ActionController
import com.hendraanggrian.openpss.ui.ConfirmDialog
import com.hendraanggrian.openpss.ui.Refreshable
import com.hendraanggrian.openpss.util.currencyCell
import com.hendraanggrian.openpss.util.doneCell
import com.hendraanggrian.openpss.util.stringCell
import java.net.URL
import java.util.ResourceBundle
import javafx.beans.property.SimpleObjectProperty
import javafx.fxml.FXML
import javafx.geometry.Side.BOTTOM
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.RadioButton
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ktfx.and
import ktfx.callbackBindingOf
import ktfx.collections.emptyObservableList
import ktfx.collections.toMutableObservableList
import ktfx.collections.toObservableList
import ktfx.controls.columns
import ktfx.controls.constrained
import ktfx.controls.isSelected
import ktfx.controls.notSelectedBinding
import ktfx.controls.selectedBinding
import ktfx.controlsfx.layouts.masterDetailPane
import ktfx.coroutines.onAction
import ktfx.coroutines.onHiding
import ktfx.coroutines.onMouseClicked
import ktfx.eq
import ktfx.inputs.isDoubleClick
import ktfx.jfoenix.controls.jfxSnackbar
import ktfx.jfoenix.layouts.leftItems
import ktfx.layouts.NodeManager
import ktfx.layouts.contextMenu
import ktfx.layouts.label
import ktfx.layouts.separatorMenuItem
import ktfx.layouts.tableView
import ktfx.neq
import ktfx.runLater
import ktfx.toBinding
import ktfx.toStringBinding
import org.joda.time.LocalDate

class InvoiceController : ActionController(), Refreshable {

    @FXML lateinit var filterBox: HBox
    @FXML lateinit var allDateRadio: RadioButton
    @FXML lateinit var pickDateRadio: RadioButton
    @FXML lateinit var dateBox: DateBox
    @FXML lateinit var customerField: TextField
    @FXML lateinit var paymentCombo: ComboBox<String>
    @FXML lateinit var typeCombo: ComboBox<String>
    @FXML lateinit var invoicePagination: PaginatedPane

    private lateinit var refreshButton: Button
    private lateinit var addButton: Button
    private lateinit var clearFiltersButton: Button
    private lateinit var searchField: IntField

    private val customerProperty = SimpleObjectProperty<Customer>()
    private lateinit var invoiceTable: TableView<Invoice>
    private lateinit var paymentTable: TableView<Payment>

    override fun NodeManager.onCreateActions() {
        refreshButton = addChild(Action(getString(R2.string.refresh), R.image.action_refresh).apply {
            onAction { refresh() }
        })
        addButton = addChild(Action(getString(R2.string.add), R.image.action_add).apply {
            onAction { addInvoice() }
        })
        clearFiltersButton = addChild(Action(getString(R2.string.clear_filters), R.image.action_clear_filters).apply {
            onAction { clearFilters() }
        })
        searchField = addChild(IntField().apply {
            filterBox.disableProperty().bind(valueProperty() neq 0)
            promptText = getString(R2.string.search_no)
        })
    }

    override fun initialize(location: URL, resources: ResourceBundle) {
        super.initialize(location, resources)
        paymentCombo.run {
            items = listOf(R2.string.paid_and_unpaid, R2.string.paid, R2.string.unpaid)
                .map { getString(it) }
                .toObservableList()
            selectionModel.selectFirst()
        }
        typeCombo.run {
            items = listOf(R2.string.offset, R2.string.plate, R2.string.digital)
                .map { getString(it) }
                .toObservableList()
            selectionModel.selectFirst()
        }
        customerField.textProperty().bind(customerProperty.toStringBinding {
            it?.toString() ?: getString(R2.string.search_customer)
        })
        dateBox.disableProperty().bind(!pickDateRadio.selectedProperty())
        clearFiltersButton.disableProperty().bind(
            pickDateRadio.selectedProperty() and
                (dateBox.valueProperty() eq LocalDate.now()) and
                customerProperty.isNull and
                (paymentCombo.selectionModel.selectedIndexProperty() eq 0) and
                (typeCombo.selectionModel.selectedIndexProperty() eq 0)
        )
    }

    override fun refresh() = runLater {
        invoicePagination.contentFactoryProperty().bind(callbackBindingOf(
            searchField.valueProperty(),
            customerProperty,
            paymentCombo.valueProperty(),
            typeCombo.valueProperty(),
            allDateRadio.selectedProperty(),
            pickDateRadio.selectedProperty(),
            dateBox.valueProperty()
        ) { (page, count) ->
            masterDetailPane(BOTTOM) {
                masterNode = ktfx.layouts.tableView<Invoice> {
                    constrained()
                    columns {
                        getString(R2.string.id)<String> { stringCell { no.toString() } }
                        getString(R2.string.date)<String> {
                            stringCell { dateTime.toString(PATTERN_DATETIMEEXT) }
                        }
                        getString(R2.string.employee)<String> {
                            stringCell {
                                runBlocking(Dispatchers.IO) {
                                    OpenPSSApi.getEmployee(employeeId).name
                                }
                            }
                        }
                        getString(R2.string.customer)<String> {
                            stringCell {
                                runBlocking(Dispatchers.IO) {
                                    OpenPSSApi.getCustomer(customerId).name
                                }
                            }
                        }
                        getString(R2.string.total)<String> { currencyCell(this@InvoiceController) { total } }
                        getString(R2.string.print)<Boolean> { doneCell { isPrinted } }
                        getString(R2.string.paid)<Boolean> { doneCell { isPaid } }
                        getString(R2.string.done)<Boolean> { doneCell { isDone } }
                    }
                    onMouseClicked {
                        if (it.isDoubleClick() && invoiceTable.selectionModel.isSelected()) {
                            viewInvoice()
                        }
                    }
                    titleProperty().bind(selectionModel.selectedItemProperty().toStringBinding {
                        Invoice.no(this@InvoiceController, it?.no)
                    })
                }.also { invoiceTable = it }
                showDetailNodeProperty().bind(invoiceTable.selectionModel.notSelectedBinding)
                detailNode = ktfx.layouts.vbox {
                    addChild(Toolbar().apply {
                        leftItems {
                            label(getString(R2.string.payment)) {
                                styleClass.addAll(R.style.bold, R.style.accent)
                            }
                        }
                    })
                    paymentTable = tableView {
                        constrained()
                        columns {
                            getString(R2.string.date)<String> {
                                stringCell { dateTime.toString(PATTERN_DATETIMEEXT) }
                            }
                            getString(R2.string.employee)<String> {
                                stringCell {
                                    runBlocking(Dispatchers.IO) {
                                        OpenPSSApi.getEmployee(employeeId).name
                                    }
                                }
                            }
                            getString(R2.string.value)<String> {
                                currencyCell(this@InvoiceController) { value }
                            }
                            getString(R2.string.cash)<Boolean> {
                                doneCell { isCash() }
                            }
                            getString(R2.string.reference)<String> {
                                stringCell { reference }
                            }
                        }
                        itemsProperty().bind(invoiceTable.selectionModel.selectedItemProperty().toBinding {
                            when (it) {
                                null -> emptyObservableList()
                                else -> runBlocking(Dispatchers.IO) {
                                    OpenPSSApi.getPayments(invoiceTable.selectionModel.selectedItem.id)
                                        .toObservableList()
                                }
                            }
                        })
                        contextMenu {
                            getString(R2.string.add)(ImageView(R.image.menu_add)) {
                                disableProperty().bind(invoiceTable.selectionModel.notSelectedBinding)
                                onAction { addPayment() }
                            }
                            getString(R2.string.delete)(ImageView(R.image.menu_delete)) {
                                disableProperty().bind(!this@tableView.selectionModel.selectedBinding)
                                onAction { deletePayment() }
                            }
                        }
                    }
                }
                dividerPosition = 0.6
                runBlocking {
                    val (pageCount, invoices) = withContext(Dispatchers.IO) {
                        OpenPSSApi.getInvoices(
                            searchField.value,
                            customerProperty.value?.name,
                            when (paymentCombo.value) {
                                getString(R2.string.paid) -> true
                                getString(R2.string.unpaid) -> false
                                else -> null
                            },
                            null,
                            when {
                                pickDateRadio.isSelected -> null
                                else -> dateBox.value
                            },
                            page,
                            count
                        )
                    }
                    invoicePagination.pageCount = pageCount
                    invoiceTable.items = invoices.toMutableObservableList()
                }
                runLater {
                    invoiceTable.contextMenu {
                        getString(R2.string.view)(ImageView(R.image.menu_invoice)) {
                            runLater {
                                disableProperty().bind(invoiceTable.selectionModel.notSelectedBinding)
                            }
                            onAction { viewInvoice() }
                        }
                        getString(R2.string.done)(ImageView(R.image.menu_done)) {
                            runLater {
                                disableProperty().bind(
                                    invoiceTable.selectionModel.selectedItemProperty().toBinding {
                                        when {
                                            it != null && !it.isDone -> false
                                            else -> true
                                        }
                                    })
                            }
                            onAction {
                                OpenPSSApi.editInvoice(
                                    invoiceTable.selectionModel.selectedItem.apply {
                                        isDone = true
                                    }
                                )
                                refreshButton.fire()
                            }
                        }
                        separatorMenuItem()
                        getString(R2.string.delete)(ImageView(R.image.menu_delete)) {
                            disableProperty().bind(invoiceTable.selectionModel.notSelectedBinding)
                            onAction {
                                withPermission {
                                    if (OpenPSSApi.deleteInvoice(
                                            login,
                                            invoiceTable.selectionModel.selectedItem
                                        )
                                    ) {
                                        invoiceTable.items.remove(invoiceTable.selectionModel.selectedItem)
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
        invoiceTable.items.add(OpenPSSApi.addInvoice(it!!))
        invoiceTable.selectionModel.selectFirst()
    }

    private fun clearFilters() {
        customerProperty.set(null)
        pickDateRadio.isSelected = true
        dateBox.picker.value = java.time.LocalDate.now()
        paymentCombo.selectionModel.selectFirst()
    }

    @FXML fun selectCustomer() = SearchCustomerPopOver(this).show(customerField) { customerProperty.set(it) }

    private fun viewInvoice() = ViewInvoicePopOver(
        this,
        invoiceTable.selectionModel.selectedItem
    ).apply {
        onHiding {
            reload(invoiceTable.selectionModel.selectedItem)
        }
    }.show(invoiceTable)

    private fun addPayment() =
        AddPaymentPopOver(this, invoiceTable.selectionModel.selectedItem).show(paymentTable) {
            OpenPSSApi.addPayment(it!!)
            updatePaymentStatus()
            reload(invoiceTable.selectionModel.selectedItem)
        }

    private fun deletePayment() = ConfirmDialog(this).show {
        withPermission {
            ConfirmDialog(this@InvoiceController).show {
                OpenPSSApi.deletePayment(login, paymentTable.selectionModel.selectedItem)
                updatePaymentStatus()
                reload(invoiceTable.selectionModel.selectedItem)
                rootLayout.jfxSnackbar(
                    getString(R2.string.payment_deleted),
                    getLong(R.value.duration_short)
                )
            }
        }
    }

    private suspend fun updatePaymentStatus() {
        OpenPSSApi.editInvoice(invoiceTable.selectionModel.selectedItem.apply {
            isPaid = total - OpenPSSApi.getPaymentDue(id) <= 0.0
        })
    }

    private suspend fun reload(invoice: Invoice) = invoiceTable.run {
        items.indexOf(invoice).let { index ->
            items[index] = OpenPSSApi.getInvoice(invoice.id)
            selectionModel.select(index)
        }
    }
}
