package com.hendraanggrian.openpss.data

import com.hendraanggrian.openpss.nosql.Document
import com.hendraanggrian.openpss.schema.Wages
import kotlinx.nosql.Id

data class Wage(
    var wageId: Int,
    var daily: Int,
    var hourlyOvertime: Int
) : Document<Wages> {

    override lateinit var id: Id<String, Wages>

    companion object {
        val NOT_FOUND: Wage = Wage(0, 0, 0)
    }
}