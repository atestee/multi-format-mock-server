package cz.cvut.fit.atlasest.application

import cz.cvut.fit.atlasest.di.configureDI
import cz.cvut.fit.atlasest.exceptions.configureExceptionHandling
import cz.cvut.fit.atlasest.routing.configureRouting
import cz.cvut.fit.atlasest.utils.log
import io.ktor.server.application.Application
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options

fun createOptions(): Options {
    val options = Options()
    options.addOption("s", "schema", true, "JSON Schema")
    options.addOption("h", "help", false, "Show help")
    return options
}

fun parseCommandLineArgs(
    options: Options,
    args: Array<String>,
): CommandLine {
    val parser = DefaultParser()
    return parser.parse(options, args)
}

fun showHelp(options: Options) {
    val formatter = HelpFormatter()
    formatter.printHelp("java -jar build/libs/multi-format-mock-server-all.jar", options)
    return
}

fun main(args: Array<String>) {
    val options = createOptions()
    val cmd = parseCommandLineArgs(options, args)
    if (cmd.hasOption("h")) showHelp(options)

    val schema = cmd.getOptionValue("s")
    io.ktor.server.netty.EngineMain.main(
        if (schema == null) arrayOf() else arrayOf("-P:schema=$schema"),
    )
}

fun Application.module() {
    log.info("Starting Ktor application...")
    val schemaFile =
        if (environment.config.keys().contains("schema")) {
            environment.config.property("schema").getString()
        } else {
            null
        }
    configureExceptionHandling()
    configureDI(loadAppConfig(), schemaFile)
    configureRouting()
    log.info("Application started successfully!")
}
