package cz.cvut.fit.atlasest.di

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.service.CollectionService
import cz.cvut.fit.atlasest.service.DocumentService
import cz.cvut.fit.atlasest.service.ParameterService
import cz.cvut.fit.atlasest.service.SchemaService
import org.koin.dsl.module

val appModule = { appConfig: AppConfig, schemaFilename: String? ->
    module {
        single { appConfig }
        single { DocumentService(appConfig.isTest) }
        single { SchemaService() }
        single { CollectionService(get(), schemaFilename, get(), get()) }
        single { ParameterService() }
    }
}
