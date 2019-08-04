package cz.jtalas

import com.google.gson.Gson
import cz.jtalas.model.ServerParams
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.request.receive
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import jdk.nashorn.internal.runtime.JSONFunctions
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author josef.talas@seznam.cz
 * @since 16.06.2019
 */
class BackendOperations {
    companion object {
        val logger: Logger = Logger.getLogger(BackendOperations::class.java.canonicalName)
    }
}

const val PING_SUFFIX = "_server/ping"

val client = HttpClient() {
    install(JsonFeature) {
        serializer = GsonSerializer()
    }
}

fun Routing.backendRoutes() {
    printJavaVersionRoute()
    runSingleCommandRoute()
    restartRoute()
    pingRoute()
}

fun backendRoute(route: String): String = "/_adminApi$route"

fun Routing.restartRoute() {
    get(backendRoute("/restart")) {
        // TODO security
        runCommand("./getFirstPackage.sh")
    }
}

fun Routing.pingRoute() {
    route(backendRoute("/ping")) {
        post {
            val json = call.receive<String>()
            val params = Gson().fromJson(json, ServerParams::class.java)

            val url = params.publicAddress + "/" + PING_SUFFIX
            val response = client.get<HttpResponse>(url)

            BackendOperations.logger.log(Level.FINE, "response status: $response.statusLine.statusCode")
            call.respond(response.status, response.readText())
        }
    }
}

fun runCommand(command: String, workingDir: File? = null): String {
    return try {
        val parts = command.split("\\s".toRegex())
        var proc = ProcessBuilder(*parts.toTypedArray())

        if (workingDir != null) {
            proc = proc.directory(workingDir)
        }

        val process = proc.redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        process.waitFor(1, TimeUnit.MINUTES)
        process.exitValue().toString()
        //process.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.message ?: "error"
    }
}

fun Routing.runSingleCommandRoute() {
    post("/run") {
        val jsonElement = json.parseJson(call.receiveText())
        val cmd = jsonElement.jsonObject["command"].toString()
        call.respondText(runCommand(cmd))
    }
}

fun Routing.printJavaVersionRoute() {
    get("/java-version") {
        val version = runCommand("java -version")
        call.respondText(version)
    }
}