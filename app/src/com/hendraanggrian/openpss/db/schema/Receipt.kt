package com.hendraanggrian.openpss.db.schema

import com.hendraanggrian.openpss.db.Document
import com.hendraanggrian.openpss.db.OrderListColumn
import com.hendraanggrian.openpss.db.Priced
import com.hendraanggrian.openpss.db.Order
import com.hendraanggrian.openpss.db.SplitPriced
import com.hendraanggrian.openpss.db.Typed
import com.hendraanggrian.openpss.db.dbDateTime
import kotlinx.nosql.Id
import kotlinx.nosql.ListColumn
import kotlinx.nosql.boolean
import kotlinx.nosql.dateTime
import kotlinx.nosql.double
import kotlinx.nosql.id
import kotlinx.nosql.integer
import kotlinx.nosql.mongodb.DocumentSchema
import kotlinx.nosql.string
import org.joda.time.DateTime

object Receipts : DocumentSchema<Receipt>("receipts", Receipt::class) {
    val employeeId = id("employee_id", Employees)
    val customerId = id("customer_id", Customers)
    val dateTime = dateTime("date_time")
    val plates = PlateColumn()
    val offsets = OffsetColumn()
    val others = OtherColumn()
    val total = double("total")
    val note = string("note")
    val payments = PaymentColumn()
    val printed = boolean("printed")

    class PlateColumn : OrderListColumn<Plate, Receipts>("plates", Plate::class) {
        val type = string("type")
        val price = double("price")
    }

    class OffsetColumn : OrderListColumn<Offset, Receipts>("offsets", Offset::class) {
        val type = string("type")
        val minQty = integer("min_qty")
        val minPrice = double("min_price")
        val excessPrice = double("excess_price")
    }

    class OtherColumn : OrderListColumn<Other, Receipts>("others", Other::class) {
        val price = double("price")
    }

    class PaymentColumn : ListColumn<Payment, Receipts>("payments", Payment::class) {
        val employeeId = id("employee_id", Employees)
        val value = double("value")
        val dateTime = dateTime("date_time")
    }
}

data class Receipt(
    val dateTime: DateTime,
    var plates: List<Plate>,
    var offsets: List<Offset>,
    var others: List<Other>,
    var total: Double,
    var note: String,
    var payments: List<Payment>,
    var printed: Boolean
) : Document<Receipts> {

    override lateinit var id: Id<String, Receipts>
    lateinit var employeeId: Id<String, Employees>
    lateinit var customerId: Id<String, Customers>

    fun isPaid(): Boolean = payments.sumByDouble { it.value } >= total

    companion object {
        fun new(
            dateTime: DateTime,
            plates: List<Plate>,
            offsets: List<Offset>,
            others: List<Other>,
            note: String,
            total: Double
        ): Receipt = Receipt(dateTime, plates, offsets, others, total, note, listOf(), false)
    }
}

data class Plate(
    override var type: String,
    override var title: String,
    override var qty: Int,
    override var price: Double,
    override var total: Double
) : Typed, Order, Priced {

    companion object {
        fun new(
            type: String,
            title: String,
            qty: Int,
            price: Double
        ): Plate = Plate(type, title, qty, price, qty * price)
    }
}

data class Offset(
    override var type: String,
    override var title: String,
    override var qty: Int,
    override var minQty: Int,
    override var minPrice: Double,
    override var excessPrice: Double,
    override var total: Double
) : Typed, Order, SplitPriced {

    companion object {
        fun new(
            type: String,
            title: String,
            qty: Int,
            minQty: Int,
            minPrice: Double,
            excessPrice: Double
        ): Offset = Offset(type, title, qty, minQty, minPrice, excessPrice, when {
            qty <= minQty -> minPrice
            else -> minPrice + ((qty - minQty) * excessPrice)
        })
    }
}

data class Other(
    override var title: String,
    override var qty: Int,
    override var price: Double,
    override var total: Double
) : Order, Priced {

    companion object {
        fun new(
            title: String,
            qty: Int,
            price: Double,
            total: Double = qty * price
        ): Other = Other(title, qty, price, total)
    }
}

data class Payment(
    var value: Double,
    val dateTime: DateTime
) {

    lateinit var employeeId: Id<String, Employees>

    companion object {
        fun new(value: Double): Payment = Payment(value, dbDateTime)
    }
}