package com.assessment.minilogbook.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the SQLCipher database passphrase using Android Keystore directly.
 *
 */
object PassphraseManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    internal const val KEY_ALIAS = "MiniLogbook_MasterKey"
    internal const val PREFS_NAME = "secure_storage"
    private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
    private const val KEY_IV = "encryption_iv"
    private const val PASSPHRASE_SIZE = 32

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedData = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivData = prefs.getString(KEY_IV, null)

        if (encryptedData != null && ivData != null) {
            try {
                val encrypted = Base64.decode(encryptedData, Base64.DEFAULT)
                val iv = Base64.decode(ivData, Base64.DEFAULT)
                return decrypt(encrypted, iv)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallthrough to generate new passphrase if decryption fails
            }
        }

        return generateAndSavePassphrase(context)
    }

    private fun generateAndSavePassphrase(context: Context): ByteArray {
        val passphrase = ByteArray(PASSPHRASE_SIZE)
        SecureRandom().nextBytes(passphrase)

        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encrypted = cipher.doFinal(passphrase)
        val iv = cipher.iv

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encrypted, Base64.DEFAULT))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.DEFAULT))
            .apply()

        return passphrase
    }

    private fun decrypt(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        return generateKey()
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)

        // Try StrongBox first (API 28+ only, requires dedicated security chip)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                keyGenerator.init(buildKeyGenSpec(true))
                return keyGenerator.generateKey()
            } catch (e: Exception) {
                // Fallback to standard TEE if StrongBox is not available
            }
        }

        // Standard TEE-backed AES-256-GCM key
        keyGenerator.init(buildKeyGenSpec(false))
        return keyGenerator.generateKey()
    }

    private fun buildKeyGenSpec(isStrongBox: Boolean): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            builder.setIsStrongBoxBacked(isStrongBox)
        }

        return builder.build()
    }
}
