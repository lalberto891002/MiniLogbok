package com.assessment.minilogbook.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Manages the SQLCipher database passphrase.
 *
 * Key storage strategy:
 * - Android 9+ (API 28) with StrongBox: key lives in a dedicated security chip (Titan M / SE)
 * - Android 6+ (API 23) without StrongBox: key lives in the TEE (hardware Keystore)
 * - Both cases: key never leaves the secure hardware in plaintext
 *
 * The passphrase itself is stored in [EncryptedSharedPreferences] encrypted with the Keystore key.
 */
object PassphraseManager {

    private const val PREFS_FILE = "db_secure_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"
    private const val PASSPHRASE_BYTE_LENGTH = 32
    private const val MASTER_KEY_ALIAS = "minilogbook_master_key"

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val masterKey = buildMasterKey(context)

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val stored = prefs.getString(KEY_PASSPHRASE, null)
        if (stored != null) {
            return Base64.decode(stored, Base64.DEFAULT)
        }

        val passphrase = ByteArray(PASSPHRASE_BYTE_LENGTH)
        SecureRandom().nextBytes(passphrase)
        prefs.edit {
            putString(KEY_PASSPHRASE, Base64.encodeToString(passphrase, Base64.DEFAULT))
        }

        return passphrase
    }

    /**
     * Builds a [MasterKey] backed by the strongest available hardware on the device:
     * - API 28+: requests StrongBox (dedicated security chip, e.g. Titan M)
     * - API 28+ without StrongBox / API < 28: falls back to TEE-backed Keystore key
     */
    private fun buildMasterKey(context: Context): MasterKey {
        // Try StrongBox first (API 28+ only, requires dedicated security chip)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val spec = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setIsStrongBoxBacked(true)   // dedicated security chip
                    .build()

                return MasterKey.Builder(context, MASTER_KEY_ALIAS)
                    .setKeyGenParameterSpec(spec)
                    .build()
            } catch (_: Exception) {
                // StrongBox not available on this device — fall through to TEE
            }
        }

        // Standard TEE-backed AES-256-GCM key (hardware Keystore, API 23+)
        return MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
}
