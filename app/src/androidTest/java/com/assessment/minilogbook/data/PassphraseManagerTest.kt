package com.assessment.minilogbook.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import org.junit.Before

@RunWith(AndroidJUnit4::class)
class PassphraseManagerTest {

    private val TEST_PREFS_NAME = "test_secure_storage"
    private val TEST_KEY_ALIAS = "MiniLogbook_TestKey"

    @Before
    fun cleanup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Clear prefs
        context.getSharedPreferences(TEST_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        // Clear Keystore alias if exists
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (keyStore.containsAlias(TEST_KEY_ALIAS)) {
            keyStore.deleteEntry(TEST_KEY_ALIAS)
        }
    }

    @Test
    fun getOrCreatePassphrase_returnsConsistency() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Generate first passphrase
        val passphrase1 = PassphraseManager.getOrCreatePassphrase(context, TEST_PREFS_NAME, TEST_KEY_ALIAS)
        assertNotNull(passphrase1)
        assertEquals(32, passphrase1.size)

        // 2. Retrieve it again
        val passphrase2 = PassphraseManager.getOrCreatePassphrase(context, TEST_PREFS_NAME, TEST_KEY_ALIAS)
        assertArrayEquals("Passphrase should be consistent across calls", passphrase1, passphrase2)
    }

    @Test
    fun getOrCreatePassphrase_generatesNewIfPrefsCleared() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Generate first passphrase
        val passphrase1 = PassphraseManager.getOrCreatePassphrase(context, TEST_PREFS_NAME, TEST_KEY_ALIAS)

        // 2. Clear the SharedPreferences where the encrypted blob is stored
        context.getSharedPreferences(TEST_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        // 3. Generate new passphrase
        val passphrase2 = PassphraseManager.getOrCreatePassphrase(context, TEST_PREFS_NAME, TEST_KEY_ALIAS)

        // 4. They should be different
        assertFalse("Passphrase should be different after clearing storage", passphrase1.contentEquals(passphrase2))
    }

    @Test
    fun keystoreEntryExists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PassphraseManager.getOrCreatePassphrase(context, TEST_PREFS_NAME, TEST_KEY_ALIAS)

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        assertTrue(keyStore.containsAlias(TEST_KEY_ALIAS))
    }
}
