package com.assessment.minilogbook

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Custom test runner that replaces [MiniLogbookApplication] with [TestApplication]
 * so Koin is NOT started automatically before tests run.
 */
class MiniLogbookTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader,
        className: String,
        context: Context
    ): Application = super.newApplication(cl, TestApplication::class.java.name, context)
}

