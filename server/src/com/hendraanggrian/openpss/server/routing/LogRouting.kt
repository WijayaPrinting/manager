package com.hendraanggrian.openpss.server.routing

import com.hendraanggrian.openpss.content.Page
import com.hendraanggrian.openpss.db.schemas.Logs
import com.hendraanggrian.openpss.server.db.transaction
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.route
import kotlin.math.ceil

fun Routing.routeLog() {
    route("log") {
        get {
            val page = call.parameters["page"]!!.toInt()
            val count = call.parameters["count"]!!.toInt()
            call.respond(
                transaction {
                    val logs = Logs()
                    Page(
                        ceil(logs.count() / count.toDouble()).toInt(),
                        logs.skip(count * page).take(count).toList()
                    )
                }
            )
        }
    }
}