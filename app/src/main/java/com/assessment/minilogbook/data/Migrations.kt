package com.assessment.minilogbook.data

import androidx.room.migration.Migration

/**
 * Central registry for all Room schema migrations.
 *
 * ## How to add a migration
 * 1. Bump `version` in the [@Database][androidx.room.Database] annotation on [GlucoseDatabase].
 * 2. Add a new [Migration] constant below following the naming convention `MIGRATION_X_Y`.
 * 3. Register it in the [all] array so [GlucoseDatabase] picks it up automatically.
 * 4. After building, commit the newly generated `app/schemas/<version>.json` file to
 *    version control so [androidx.room.testing.MigrationTestHelper] can validate it.
 *
 * ## Example — adding a `notes` column in v2
 * ```kotlin
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE glucose_entries ADD COLUMN notes TEXT")
 *     }
 * }
 * ```
 *
 * ## Why not fallbackToDestructiveMigration?
 * MiniLogbook stores health data entered by the user. Silent data loss is unacceptable, so
 * [androidx.room.RoomDatabase.Builder.fallbackToDestructiveMigration] is intentionally **not**
 * used for upgrades. If a migration is missing Room will throw an
 * [IllegalStateException] at startup — a loud, obvious signal that the migration was forgotten.
 *
 * [androidx.room.RoomDatabase.Builder.fallbackToDestructiveMigrationOnDowngrade] **is** enabled
 * as a safety valve: running an older APK against a newer database is an unsupported scenario,
 * and wiping is safer than corrupting data in that edge case.
 *
 * ## How to test a migration
 * No migration tests are needed for v1 — there is nothing to migrate yet.
 * Add an instrumented test in `androidTest/` **for every new migration** using
 * [androidx.room.testing.MigrationTestHelper]:
 * ```kotlin
 * @RunWith(AndroidJUnit4::class)
 * class MigrationTest {
 *     @get:Rule
 *     val helper = MigrationTestHelper(
 *         InstrumentationRegistry.getInstrumentation(),
 *         GlucoseDatabase::class.java
 *     )
 *
 *     @Test
 *     fun migrate1To2() {
 *         helper.createDatabase(TEST_DB, 1).close()
 *         val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
 *         // assert data integrity here
 *     }
 * }
 * ```
 * `MigrationTestHelper` reads the schema JSON files from `app/schemas/` (packaged as
 * androidTest assets) to verify the resulting schema matches the expected one.
 */
object Migrations {

    // -----------------------------------------------------------------------------------------
    // Add migration objects here as the schema evolves.
    // -----------------------------------------------------------------------------------------
    // val MIGRATION_1_2 = object : Migration(1, 2) { ... }

    /**
     * All registered migrations, passed to
     * [androidx.room.RoomDatabase.Builder.addMigrations].
     * Keep this list in ascending version order.
     */
    val all: Array<Migration> = arrayOf(
        // MIGRATION_1_2,
    )
}

