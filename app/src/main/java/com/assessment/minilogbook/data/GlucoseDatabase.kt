package com.assessment.minilogbook.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Room database for MiniLogbook, encrypted at rest with SQLCipher (AES-256).
 *
 * The database is exposed as a process-wide singleton via [getDatabase]. The passphrase
 * is managed by [PassphraseManager], which generates a 32-byte random key on first launch,
 * encrypts it with an AES-256-GCM key stored in the Android Keystore, and persists the
 * ciphertext in [android.content.SharedPreferences].
 *
 * Only one entity is currently registered: [GlucoseEntry]. Bump [version] and supply a
 * [androidx.room.migration.Migration] whenever the schema changes.
 *
 * Schema JSON files are exported to `app/schemas/` (configured via `room.schemaLocation` in
 * `build.gradle.kts`). Commit those files to version control so that
 * [androidx.room.testing.MigrationTestHelper] can load them when writing migration tests.
 */
@Database(entities = [GlucoseEntry::class], version = 1, exportSchema = true)
abstract class GlucoseDatabase : RoomDatabase() {

    /** Returns the DAO used to read and write [GlucoseEntry] records. */
    abstract fun glucoseDao(): GlucoseDao

    companion object {
        @Volatile
        private var INSTANCE: GlucoseDatabase? = null

        /**
         * Returns the singleton [GlucoseDatabase] instance, creating it on the first call.
         *
         * SQLCipher native libraries are loaded here (before [Room.databaseBuilder]) to
         * ensure the [SupportOpenHelperFactory] can find them at runtime.
         *
         * @param context Any [Context]; `applicationContext` is used internally to avoid leaks.
         * @return The application-scoped [GlucoseDatabase] singleton.
         */
        fun getDatabase(context: Context): GlucoseDatabase {
            return INSTANCE ?: synchronized(this) {
                // Load the SQLCipher native libraries before building the database
                System.loadLibrary("sqlcipher")

                val passphrase = PassphraseManager.getOrCreatePassphrase(context)
                val factory = SupportOpenHelperFactory(passphrase)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GlucoseDatabase::class.java,
                    "glucose_database"
                )
                    .openHelperFactory(factory)
                    .addMigrations(*Migrations.all)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
