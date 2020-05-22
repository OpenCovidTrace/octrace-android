package org.opencovidtrace.octrace.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.opencovidtrace.octrace.data.BtLogTableValue
import org.opencovidtrace.octrace.data.Dp3tLogTableValue

@Dao
interface OctraceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertBtLog(btLogTableValue: BtLogTableValue): Long

    @Query("SELECT * from bt_log_table ORDER BY time DESC")
    fun getBtLogsLiveData(): LiveData<List<BtLogTableValue>>

    @Query("DELETE FROM bt_log_table")
    fun clearBtLogs()


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDp3tLog(dp3tLogTableValue: Dp3tLogTableValue): Long

    @Query("SELECT * from dp3t_log_table ORDER BY time DESC")
    fun getDp3tLogsLiveData(): LiveData<List<Dp3tLogTableValue>>

    @Query("DELETE FROM dp3t_log_table")
    fun clearDp3tLogs()

}
