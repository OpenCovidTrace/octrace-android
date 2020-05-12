package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.data.ContactCoord
import org.opencovidtrace.octrace.data.ContactMetaData
import org.opencovidtrace.octrace.utils.CryptoUtil
import org.opencovidtrace.octrace.utils.CryptoUtil.base64DecodeByteArray
import org.opencovidtrace.octrace.utils.CryptoUtil.base64EncodedString

object QrContactsManager : PreferencesHolder("qr-contacts") {

    private const val CONTACTS = "contacts"

    fun getContacts(): List<QrContact> {
        val jsonString = getString(CONTACTS)
        (Gson().fromJson(jsonString) as? List<QrContact>)?.let {
            return it
        } ?: kotlin.run { return arrayListOf() }
    }

    fun setContacts(newValue: List<QrContact>) {
        val hashMapString = Gson().toJson(newValue)
        setString(CONTACTS, hashMapString)
    }

    fun removeOldContacts() {
        val expirationDay = DataManager.expirationDay()

        val newContacts = getContacts().filter { it.day > expirationDay }

        setContacts(newContacts)
    }

    fun matchContacts(keysData: KeysData): Pair<Boolean, ContactCoord?> {
        val newContacts = getContacts()

        var hasExposure = false
        var lastExposedContactCoord: ContactCoord? = null

        newContacts.forEach { contact ->
            keysData.keys.filter {
                it.day == contact.day
            }.forEach { key ->
                if (CryptoUtil.match(contact.rollingId, contact.day, key.value.toByteArray())) {
                    contact.exposed = true

                    key.meta?.let { metaKey ->
                        contact.metaData = CryptoUtil.decodeMetaData(
                            contact.meta.toByteArray(),
                            metaKey.toByteArray()
                        )

                        contact.metaData?.coord?.let {
                            lastExposedContactCoord = it
                        }
                    }

                    hasExposure = true
                }
            }
        }

        setContacts(newContacts)

        return Pair(hasExposure, lastExposedContactCoord)
    }

    fun addContact(contact: QrContact) {
        val newContacts = getContacts().toMutableList()

        newContacts.add(contact)

        setContacts(newContacts)
    }

}


data class QrContact(
    val rollingId: String,
    val meta: String,
    val day: Int = CryptoUtil.currentDayNumber(),
    var exposed: Boolean = false,
    var metaData: ContactMetaData? = null
) {
    companion object {
        fun create(rpi: String): QrContact {
            val rpiData = rpi.base64DecodeByteArray()

            return QrContact(
                rpiData.sliceArray(0 until CryptoUtil.keyLength).base64EncodedString(),
                rpiData.sliceArray(CryptoUtil.keyLength until CryptoUtil.keyLength * 2)
                    .base64EncodedString()
            )
        }
    }
}
