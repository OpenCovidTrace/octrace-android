package org.opencovidtrace.octrace.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.opencovidtrace.octrace.data.BtContactHealth
import org.opencovidtrace.octrace.data.LogTableValue

@Dao
interface OctraceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLog(logTableValue: LogTableValue): Long

    @Query("SELECT * from log_table ORDER BY time DESC")
    fun getLogsLiveData(): LiveData<List<LogTableValue>>

    @Query("DELETE FROM log_table")
    fun clearLogs()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertContact(contact: BtContactHealth): Long

    @Query("SELECT * FROM contact_health_table")
    fun loadAllContacts(): List<BtContactHealth>

    @Query("DELETE FROM contact_health_table WHERE contact_encounters_tst<:expTimestamp")
    fun removeOldContacts(expTimestamp: Long)

}
