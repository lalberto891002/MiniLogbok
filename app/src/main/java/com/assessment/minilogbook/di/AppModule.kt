package com.assessment.minilogbook.di

import com.assessment.minilogbook.data.GlucoseDatabase
import com.assessment.minilogbook.domain.service.GlucoseService
import com.assessment.minilogbook.domain.service.IGlucoseService
import com.assessment.minilogbook.ui.viewmodel.GlucoseViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    // Singleton for the Database
    single { GlucoseDatabase.getDatabase(androidContext()) }

    // Singleton for the DAO
    single { get<GlucoseDatabase>().glucoseDao() }

    // Bind the concrete GlucoseService to the IGlucoseService interface
    factory { GlucoseService() } bind IGlucoseService::class

    // Define ViewModel using constructor DSL from core module
    viewModelOf(::GlucoseViewModel)
}
