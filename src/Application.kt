package cz.jtalas

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.ContentType
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

fun Application.module() {
    routing {
        //trace { application.log.trace(it.buildText()) }
        htmlRoute()
        backendRoutes()
        adminUiRoute()
    }
}

val json = Json(JsonConfiguration.Stable)

/*
fun main(args: Array<String>) {
    val config = HoconApplicationConfig(ConfigFactory.load())

    val server = embeddedServer(Netty, port = 8080, watchPaths = listOf("src"), module = Application::module)
    server.start(wait = true)
}
 */

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Routing.htmlRoute() {
    get("/html") {
        call.respondText("<h1>Hello World!</h1>", contentType = ContentType.Text.Html)
    }
}

fun Routing.adminUiRoute() {
    static("") {
        files("admin-ui/build")
    }
}