package com.hendraanggrian.openpss.db.schema

import com.hendraanggrian.openpss.db.NamedDocument
import com.hendraanggrian.openpss.db.NamedDocumentSchema
import com.hendraanggrian.openpss.db.dbDate
import javafx.collections.ObservableList
import kotlinx.nosql.Id
import kotlinx.nosql.ListColumn
import kotlinx.nosql.date
import kotlinx.nosql.string
import ktfx.collections.observableListOf
import org.joda.time.LocalDate

object Customers : NamedDocumentSchema<Customer>("customers", Customer::class) {
    val note = string("note")
    val since = date("since")
    val contacts = ContactColumn()

    class ContactColumn : ListColumn<Contact, Customers>("contacts", Contact::class) {
        val type = string("type")
        val value = string("value")
    }
}

data class Customer @JvmOverloads constructor(
    override var name: String,
    var note: String = "",
    val since: LocalDate = dbDate,
    var contacts: List<Contact> = listOf()
) : NamedDocument<Customers> {

    override lateinit var id: Id<String, Customers>

    override fun toString(): String = name
}

data class Contact(
    var type: String,
    var value: String
) {
    companion object {
        private const val TYPE_EMAIL = "email"
        private const val TYPE_PHONE = "phone"

        fun listTypes(): ObservableList<String> = observableListOf(TYPE_EMAIL, TYPE_PHONE)
    }
}