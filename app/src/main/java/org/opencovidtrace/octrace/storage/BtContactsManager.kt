package org.opencovidtrace.octrace.storage

import org.opencovidtrace.octrace.data.BtContact
import org.opencovidtrace.octrace.data.BtContactHealth
import org.opencovidtrace.octrace.data.BtEncounter
import org.opencovidtrace.octrace.di.DatabaseProvider
import org.opencovidtrace.octrace.utils.DoAsync

object BtContactsManager {

    private val database by DatabaseProvider()

    fun removeOldContacts() {
        DoAsync { database.appDao().removeOldContacts(DataManager.expirationTimestamp()) }
    }

    fun addContact(id: String, encounter: BtEncounter) {
        val bch = BtContactHealth(BtContact(id, encounter))
        DoAsync { database.appDao().insertContact(bch) }
    }

    fun fetchContacts(contactsCallback: (List<BtContactHealth>) -> Unit) {
        DoAsync { contactsCallback(database.appDao().loadAllContacts()) }
    }

}

