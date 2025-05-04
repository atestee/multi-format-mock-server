package cz.cvut.fit.atlasest.application

import cz.cvut.fit.atlasest.di.configureDI
import cz.cvut.fit.atlasest.exceptionHandling.configureExceptionHandling
import cz.cvut.fit.atlasest.routing.configureRouting
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options

fun defineCliOptions(args: Array<String>): CommandLine {
    val options =
        Options().apply {
            this.addOption("s", "schema", true, "JSON Schema")
            this.addOption("l", "limit", true, "Pagination limit")
            this.addOption("i", "identifier", true, "Default identifier")
            this.addOption("h", "help", false, "Show help")
        }
    val parser = DefaultParser()
    val cmd = parser.parse(options, args)
    if (cmd.hasOption("h")) {
        val formatter = HelpFormatter()
        formatter.printHelp("java -jar build/libs/multi-format-mock-server-all.jar", options)
    }
    return cmd
}

fun main(args: Array<String>) {
    val cmd = defineCliOptions(args)
    val schema = cmd.getOptionValue("s")
    val defaultPaginationLimit = cmd.getOptionValue("l")
    val defaultIdentifier = cmd.getOptionValue("i")
    if (schema != null) {
        System.setProperty("schema", schema)
    }
    if (defaultPaginationLimit != null) {
        System.setProperty("defaultPaginationLimit", defaultPaginationLimit)
    }
    if (defaultIdentifier != null) {
        System.setProperty("defaultIdentifier", defaultIdentifier)
    }
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    val appConfig = loadAppConfig()
    configureDI(appConfig)
    configureExceptionHandling()
    configureRouting()
    configureCORS()
}

fun Application.configureCORS() {
    val config = environment.config
    val allowedHost = config.property("cors.allowed-host").getString()
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        exposeHeader(HttpHeaders.ContentType)
        exposeHeader(HttpHeaders.Location)
        exposeHeader(HttpHeaders.Accept)
        exposeHeader(HttpHeaders.Link)
        exposeHeader(HttpHeaders.Vary)
        allowHost(allowedHost)
    }
}
