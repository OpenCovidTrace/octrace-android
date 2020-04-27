package org.opencovidtrace.octrace.storage

import org.opencovidtrace.octrace.data.BtContact
import org.opencovidtrace.octrace.data.BtContactHealth
import org.opencovidtrace.octrace.data.BtEncounter
import org.opencovidtrace.octrace.data.ContactWithEncounters
import org.opencovidtrace.octrace.di.DatabaseProvider
import org.opencovidtrace.octrace.utils.DoAsync

object BtContactsManager {

    private val database by DatabaseProvider()

    fun removeOldContacts() {
        DoAsync { database.appDao().removeOldContacts(DataManager.expirationTimestamp()) }
    }

    fun addContact(id: String, encounter: BtEncounter) {
        val bch = BtContactHealth(BtContact(id))
        DoAsync { database.appDao().insertContactEncounter(bch, encounter) }
    }

    fun fetchContacts(contactsCallback: (List<ContactWithEncounters>) -> Unit) {
        DoAsync { contactsCallback(database.appDao().fetchAllContactsWithEncounters()) }
    }

    fun fetchEncounters(contactId: String, contactsCallback: (List<BtEncounter>) -> Unit) {
        DoAsync { contactsCallback(database.appDao().fetchEncounters(contactId)) }
    }
}

