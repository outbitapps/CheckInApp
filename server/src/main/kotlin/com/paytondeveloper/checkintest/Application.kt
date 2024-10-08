package com.paytondeveloper.checkintest

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import io.ktor.util.getDigestFunction

val authHashFunc = getDigestFunction("SHA-256") { "330c3ba9-e924-49fc-8f83-5c0a10082b5c${it.length}" }

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(Authentication) {
        basic("basic-login") {

        }
    }
    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        
    }
}