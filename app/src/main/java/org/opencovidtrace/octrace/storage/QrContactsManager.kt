package org.opencovidtrace.octrace.storage

import com.google.gson.Gson
import org.opencovidtrace.octrace.data.KeysData
import org.opencovidtrace.octrace.data.QrContact
import org.opencovidtrace.octrace.data.QrContactHealth
import org.opencovidtrace.octrace.utils.CryptoUtil

object QrContactsManager : PreferencesHolder("qr-contacts") {

    private const val CONTACTS = "contacts"

    fun getContacts(): List<QrContactHealth> {
        val storedHashMapString = KeyManager.getString(CONTACTS)
        (Gson().fromJson(storedHashMapString) as? List<QrContactHealth>)?.let {
            return it
        } ?: kotlin.run { return arrayListOf() }
    }

    fun setContacts(newValue: List<QrContactHealth>) {
        val hashMapString = Gson().toJson(newValue)
        KeyManager.setString(CONTACTS, hashMapString)
    }

    fun removeOldContacts() {
        val expirationTimestamp = DataManager.expirationTimestamp()

        val newContacts = getContacts().filter { it.contact.tst > expirationTimestamp }

        setContacts(newContacts)
    }

    fun matchContacts(keysData: KeysData) : QrContact? {
        val newContacts = getContacts()

        var lastInfectedContact: QrContact?=null

        newContacts.forEach { contact ->
            val contactDate =contact.contact.date()
            val contactDay = CryptoUtil.getDayNumber(contactDate)

            keysData.keys.firstOrNull { it.day== contactDay &&
                    CryptoUtil.spec.match(contact.contact.id, contactDate, it.value.toByteArray())}?.let {
                contact.infected = true
                lastInfectedContact = contact.contact
            }
        }

        setContacts(newContacts)

        return lastInfectedContact
    }

    fun addContact(contact: QrContact) {
        val newContacts = getContacts().toMutableList()

        newContacts.add(QrContactHealth(contact))

        setContacts(newContacts)
    }
}