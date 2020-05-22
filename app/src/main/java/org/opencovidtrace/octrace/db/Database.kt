package org.opencovidtrace.octrace.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.opencovidtrace.octrace.data.BtLogTableValue
import org.opencovidtrace.octrace.data.Dp3tLogTableValue


@Database(entities = [BtLogTableValue::class, Dp3tLogTableValue::class], version = 1)

@TypeConverters(DatabaseConverters::class)
abstract class Database : RoomDatabase() {

    abstract fun appDao(): OctraceDao

}
