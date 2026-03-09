package com.assessment.minilogbook.di

import com.assessment.minilogbook.data.GlucoseDatabase
import com.assessment.minilogbook.domain.usecase.GlucoseService
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // Singleton for the Database
    single { GlucoseDatabase.getDatabase(androidContext()) }

    // Singleton for the DAO
    single { get<GlucoseDatabase>().glucoseDao() }

    // Define factory using constructor DSL
    factoryOf(::GlucoseService)

    // Define ViewModel using constructor DSL from core module
    viewModelOf(::GlucoseViewModel)
}
