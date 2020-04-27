package org.opencovidtrace.octrace.db

import androidx.lifecycle.LiveData
import androidx.room.*
import org.opencovidtrace.octrace.data.BtContactHealth
import org.opencovidtrace.octrace.data.BtEncounter
import org.opencovidtrace.octrace.data.ContactWithEncounters
import org.opencovidtrace.octrace.data.LogTableValue

@Dao
interface OctraceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLog(logTableValue: LogTableValue): Long

    @Query("SELECT * from log_table ORDER BY time DESC")
    fun getLogsLiveData(): LiveData<List<LogTableValue>>

    @Query("DELETE FROM log_table")
    fun clearLogs()

    @Transaction
    fun insertContactEncounter(contact: BtContactHealth, encounter: BtEncounter) {
        insertContact(contact)
        encounter.contactId = contact.contact.id
        insertEncounter(encounter)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertContact(contact: BtContactHealth): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEncounter(encounter: BtEncounter): Long

    @Query("SELECT * FROM contact_health_table")
    fun fetchAllContacts(): List<BtContactHealth>

    @Transaction
    @Query("SELECT * FROM contact_health_table")
    fun fetchAllContactsWithEncounters(): List<ContactWithEncounters>

    @Query("SELECT * FROM encounter_table WHERE contactId = :contactId")
    fun fetchEncounters(contactId: String): List<BtEncounter>

    @Query("DELETE FROM encounter_table WHERE tst<:expTimestamp")
    fun removeOldContacts(expTimestamp: Long)

}
