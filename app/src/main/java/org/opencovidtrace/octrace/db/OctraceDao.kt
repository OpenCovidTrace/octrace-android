package org.opencovidtrace.octrace.db

import androidx.lifecycle.LiveData
import androidx.room.*
import org.opencovidtrace.octrace.data.LogTableValue

@Dao
interface OctraceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLog(logTableValue: LogTableValue): Long

    @Query("SELECT * from log_table ORDER BY time DESC")
    fun getLogsLiveData(): LiveData<List<LogTableValue>>

    @Query("DELETE FROM log_table")
    fun clearLogs()

}
