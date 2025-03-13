package cz.cvut.fit.atlasest.di

import cz.cvut.fit.atlasest.application.AppConfig
import cz.cvut.fit.atlasest.data.Repository
import cz.cvut.fit.atlasest.service.JsonService
import org.koin.dsl.module

val appModule = { appConfig: AppConfig ->
    module {
        single { appConfig }
        single { JsonService() }
        single { Repository(get(), get()) }
    }
}
