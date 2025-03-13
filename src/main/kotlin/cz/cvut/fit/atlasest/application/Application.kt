package cz.cvut.fit.atlasest.application

import cz.cvut.fit.atlasest.di.configureDI
import cz.cvut.fit.atlasest.exceptions.configureExceptionHandling
import cz.cvut.fit.atlasest.routing.configureRouting
import cz.cvut.fit.atlasest.utils.log
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    log.info("Starting Ktor application...")
    configureExceptionHandling()
    configureDI(loadAppConfig())
    configureRouting()
    log.info("Application started successfully!")
}
