package com.reelscounter.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypted local counters. Encryption key is non-exportable in Android Keystore. */
class SecureStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("encrypted_counters", Context.MODE_PRIVATE)
    private val alias = "reels_counter_aes_v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
    @Synchronized fun read(): JSONObject {
        val blob = prefs.getString("blob", null) ?: return JSONObject()
        return try {
            val bytes = Base64.decode(blob, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12))
            JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
        } catch (_: Exception) { JSONObject() }
    }
    @Synchronized private fun write(json: JSONObject) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val bytes = cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
        prefs.edit().putString("blob", Base64.encodeToString(bytes, Base64.NO_WRAP)).commit()
    }
    @Synchronized fun increment(date: String, app: String) {
        val root = read(); val day = root.optJSONObject(date) ?: JSONObject()
        day.put(app, day.optInt(app, 0) + 1); root.put(date, day); write(root)
    }
    @Synchronized fun clear() { prefs.edit().clear().commit() }
}
