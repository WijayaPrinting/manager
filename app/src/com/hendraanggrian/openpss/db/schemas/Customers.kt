package com.hendraanggrian.openpss.db.schemas

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.db.Document
import com.hendraanggrian.openpss.db.Named
import com.hendraanggrian.openpss.db.NamedSchema
import com.hendraanggrian.openpss.db.dbDate
import com.hendraanggrian.openpss.ui.Resourced
import kotlinx.nosql.Id
import kotlinx.nosql.date
import kotlinx.nosql.listOfString
import kotlinx.nosql.mongodb.DocumentSchema
import kotlinx.nosql.string
import org.joda.time.LocalDate

object Customers : DocumentSchema<Customer>("customers", Customer::class), NamedSchema {
    override val name = string("name")
    val since = date("since")
    val address = string("address")
    val note = string("note")
    val phones = listOfString("phones")
    val emails = listOfString("emails")
}

data class Customer(
    override var name: String,
    val since: LocalDate,
    val address: String,
    var note: String,
    var phones: List<String>,
    var emails: List<String>
) : Document<Customers>, Named {

    companion object {
        fun new(name: String): Customer = Customer(name, dbDate, "", "", listOf(), listOf())
    }

    override lateinit var id: Id<String, Customers>

    override fun toString(): String = name

    enum class ContactType {
        PHONE, EMAIL;

        fun asString(resourced: Resourced): String = resourced.getString(when (this) {
            PHONE -> R.string.phone
            EMAIL -> R.string.email
        })
    }
}