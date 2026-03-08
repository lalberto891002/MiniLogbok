package com.assessment.minilogbook

import android.app.Application

/**
 * Test application that intentionally does NOT start Koin.
 * Koin is initialised per-test in [com.assessment.minilogbook.ui.screen.MiniLogbookScreenTest]
 * so each test gets a fresh in-memory database.
 */
class TestApplication : Application()

