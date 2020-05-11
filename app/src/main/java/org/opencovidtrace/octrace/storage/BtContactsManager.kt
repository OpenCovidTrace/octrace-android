package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.data.ContactCoord
import org.opencovidtrace.octrace.data.ContactMetaData
import org.opencovidtrace.octrace.utils.CryptoUtil

object BtContactsManager : PreferencesHolder("bt-contacts") {

    private const val CONTACTS = "contacts"

    fun getContacts(): Map<String, BtContact> {
        val jsonString = getString(CONTACTS)
        (Gson().fromJson(jsonString) as? Map<String, BtContact>)?.let {
            return it
        } ?: kotlin.run { return mapOf() }
    }

    fun setContacts(newValue: Map<String, BtContact>) {
        val hashMapString = Gson().toJson(newValue)
        setString(CONTACTS, hashMapString)
    }

    fun removeOldContacts() {
        val expirationDay = DataManager.expirationDay()

        val newContacts = getContacts().filterValues { it.day > expirationDay }

        setContacts(newContacts)
    }

    fun matchContacts(keysData: KeysData): Pair<Boolean, ContactCoord?> {
        val newContacts = getContacts()

        var hasExposure = false
        var lastExposedContactCoord: ContactCoord? = null

        newContacts.forEach { (_, contact) ->
            keysData.keys.filter {
                it.day == contact.day
            }.forEach { key ->
                if (CryptoUtil.match(contact.rollingId, contact.day, key.value.toByteArray())) {
                    contact.exposed = true

                    key.meta?.let { metaKey ->
                        contact.encounters.forEach { encounter ->
                            encounter.metaData = CryptoUtil.decodeMetaData(
                                encounter.meta.toByteArray(),
                                metaKey.toByteArray()
                            )

                            encounter.metaData?.coord?.let {
                                lastExposedContactCoord = it
                            }
                        }


                    }

                    hasExposure = true
                }
            }
        }

        setContacts(newContacts)

        return Pair(hasExposure, lastExposedContactCoord)
    }

    fun addContact(rollingId: String, day: Int, encounter: BtEncounter) {
        val newContacts = getContacts().toMutableMap()

        newContacts[rollingId]?.let {
            it.encounters += encounter
        } ?: run {
            newContacts[rollingId] = BtContact(rollingId, day, listOf(encounter))
        }

        setContacts(newContacts)
    }

}

data class BtContact(
    val rollingId: String,
    val day: Int,
    var encounters: List<BtEncounter>,
    var exposed: Boolean = false
)


data class BtEncounter(
    val rssi: Int,
    val meta: String,
    var metaData: ContactMetaData? = null
)
