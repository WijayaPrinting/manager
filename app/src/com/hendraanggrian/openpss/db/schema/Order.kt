package com.hendraanggrian.openpss.db.schema

import com.hendraanggrian.openpss.db.Document
import kotlinx.nosql.Discriminator
import kotlinx.nosql.Id
import kotlinx.nosql.double
import kotlinx.nosql.id
import kotlinx.nosql.integer
import kotlinx.nosql.mongodb.DocumentSchema
import kotlinx.nosql.string
import kotlin.reflect.KClass

sealed class Orders<D : Any, S : DocumentSchema<D>>(klass: KClass<D>, discriminator: String) : DocumentSchema<D>("order", klass, Discriminator(string("type"), discriminator)) {
    val total = double<S>("total")
}

object OffsetOrders : Orders<OffsetOrder, OffsetOrders>(OffsetOrder::class, "print")

object PlateOrders : Orders<PlateOrder, PlateOrders>(PlateOrder::class, "plate") {
    val plateId = id("plate_id", Plates)
    val qty = integer("qty")
    val price = double("price")
}

sealed class Order<D : Any, S : DocumentSchema<D>> : Document<S> {

    override lateinit var id: Id<String, S>
    open var total: Double = 0.0
}

class OffsetOrder : Order<OffsetOrder, OffsetOrders>()

data class PlateOrder(
    var plateId: Id<String, Plates>?,
    var qty: Int,
    var price: Double,
    override var total: Double
) : Order<PlateOrder, PlateOrders>() {

    override lateinit var id: Id<String, PlateOrders>
}