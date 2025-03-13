package cz.cvut.fit.atlasest.di

import cz.cvut.fit.atlasest.application.AppConfig
import org.koin.core.context.startKoin

fun configureDI(appConfig: AppConfig) {
    startKoin {
        modules(appModule(appConfig))
    }
}
