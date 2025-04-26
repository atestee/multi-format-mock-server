package cz.cvut.fit.atlasest.di

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.services.CollectionService
import cz.cvut.fit.atlasest.services.DocumentService
import cz.cvut.fit.atlasest.services.FilterService
import cz.cvut.fit.atlasest.services.PaginationService
import cz.cvut.fit.atlasest.services.ParameterService
import cz.cvut.fit.atlasest.services.SchemaService
import cz.cvut.fit.atlasest.services.SortingService
import org.koin.dsl.module

val appModule = { appConfig: AppConfig, schemaFilename: String? ->
    module {
        single { appConfig }
        single { DocumentService(appConfig.isTest) }
        single { SchemaService() }
        single { CollectionService(get(), schemaFilename, get(), get()) }
        single { FilterService() }
        single { SortingService() }
        single { PaginationService() }
        single { ParameterService(get(), get(), get(), get()) }
    }
}
