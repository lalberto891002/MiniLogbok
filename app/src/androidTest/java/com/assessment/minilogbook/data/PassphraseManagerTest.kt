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

    @Before
    fun cleanup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Clear prefs
        context.getSharedPreferences(PassphraseManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        // Clear Keystore alias if exists
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (keyStore.containsAlias(PassphraseManager.KEY_ALIAS)) {
            keyStore.deleteEntry(PassphraseManager.KEY_ALIAS)
        }
    }

    @Test
    fun getOrCreatePassphrase_returnsConsistency() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Generate first passphrase
        val passphrase1 = PassphraseManager.getOrCreatePassphrase(context)
        assertNotNull(passphrase1)
        assertEquals(32, passphrase1.size)

        // 2. Retrieve it again
        val passphrase2 = PassphraseManager.getOrCreatePassphrase(context)
        assertArrayEquals("Passphrase should be consistent across calls", passphrase1, passphrase2)
    }

    @Test
    fun getOrCreatePassphrase_generatesNewIfPrefsCleared() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Generate first passphrase
        val passphrase1 = PassphraseManager.getOrCreatePassphrase(context)

        // 2. Clear the SharedPreferences where the encrypted blob is stored
        context.getSharedPreferences(PassphraseManager.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        // 3. Generate new passphrase
        val passphrase2 = PassphraseManager.getOrCreatePassphrase(context)

        // 4. They should be different
        assertFalse("Passphrase should be different after clearing storage", passphrase1.contentEquals(passphrase2))
    }

    @Test
    fun keystoreEntryExists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PassphraseManager.getOrCreatePassphrase(context)

        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        assertTrue(keyStore.containsAlias(PassphraseManager.KEY_ALIAS))
    }
}
