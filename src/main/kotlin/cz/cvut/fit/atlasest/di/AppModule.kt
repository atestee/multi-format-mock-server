package cz.cvut.fit.atlasest.di

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.data.FileHandler
import cz.cvut.fit.atlasest.data.Repository
import cz.cvut.fit.atlasest.services.CollectionService
import cz.cvut.fit.atlasest.services.FilterService
import cz.cvut.fit.atlasest.services.PaginationService
import cz.cvut.fit.atlasest.services.ParameterService
import cz.cvut.fit.atlasest.services.SchemaService
import cz.cvut.fit.atlasest.services.SortingService
import org.koin.dsl.module

val appModule = { appConfig: AppConfig ->
    module {
        single { appConfig }
        single { FileHandler(appConfig.isTest) }
        single { SchemaService() }
        single { CollectionService(get(), get()) }
        single { Repository(get(), get(), get()) }
        single { FilterService() }
        single { SortingService() }
        single { PaginationService(appConfig.defaultLimit) }
        single { ParameterService(get(), get(), get(), get()) }
    }
}
