package com.hendraanggrian.openpss.route

import com.hendraanggrian.openpss.content.Page
import com.hendraanggrian.openpss.db.schemas.Employee
import com.hendraanggrian.openpss.db.schemas.Invoice
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpMethod
import kotlinx.nosql.Id

interface InvoiceRoute : Route {

    suspend fun getInvoices(
        search: Int = 0,
        customer: String? = null,
        isPaid: Boolean? = null,
        isDone: Boolean? = null,
        date: Any? = null,
        page: Int,
        count: Int
    ): Page<Invoice> = client.get {
        apiUrl("invoices")
        parameters(
            "search" to search,
            "customer" to customer,
            "isPaid" to isPaid,
            "date" to date,
            "page" to page,
            "count" to count
        )
    }

    suspend fun addInvoice(invoice: Invoice): Invoice = client.post {
        apiUrl("invoices")
        body = invoice
    }

    suspend fun deleteInvoice(login: Employee, invoice: Invoice): Boolean = client.requestStatus {
        apiUrl("invoices")
        method = HttpMethod.Delete
        body = invoice
        parameters("login" to login.name)
    }

    suspend fun getInvoice(id: Id<String, *>): Invoice = client.get {
        apiUrl("invoices/$id")
    }

    suspend fun editInvoice(
        invoice: Invoice,
        isPrinted: Boolean = invoice.isPrinted,
        isPaid: Boolean = invoice.isPaid,
        isDone: Boolean = invoice.isDone
    ): Boolean = client.requestStatus {
        apiUrl("invoices/${invoice.id}")
        parameters(
            "isPrinted" to isPrinted,
            "isPaid" to isPaid,
            "isDone" to isDone
        )
    }

    suspend fun nextInvoice(): Int = client.get {
        apiUrl("invoices/next")
    }
}