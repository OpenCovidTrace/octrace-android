package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.utils.CryptoUtil

object EncryptionKeysManager : PreferencesHolder("encryption-keys") {

    private const val ENCRYPTION_KEYS = "encryptionKeys"

    fun getEncryptionKeys(): HashMap<Long, ByteArray> {
        val storedHashMapString = OnboardingManager.getString(ENCRYPTION_KEYS)
        (Gson().fromJson(storedHashMapString) as? HashMap<Long, ByteArray>)?.let {
            return it
        } ?: kotlin.run { return hashMapOf() }
    }

    fun setEncryptionKeys(newValue: HashMap<Long, ByteArray>) {
        val hashMapString = Gson().toJson(newValue)
        OnboardingManager.setString(ENCRYPTION_KEYS, hashMapString)
    }

    fun removeOldKeys() {
        val expirationTimestamp = DataManager.expirationTimestamp()

        val remainingKeys = getEncryptionKeys().filterKeys { it > expirationTimestamp }

        setEncryptionKeys(HashMap(remainingKeys))
    }

    fun generateKey(tst: Long) : ByteArray {
        val key = CryptoUtil.generateKey(16)

        val newKeys = getEncryptionKeys()
        newKeys[tst] = key

        setEncryptionKeys(newKeys)

        return key
    }
}