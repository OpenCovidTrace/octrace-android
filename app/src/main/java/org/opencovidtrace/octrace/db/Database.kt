package org.opencovidtrace.octrace.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.opencovidtrace.octrace.data.BtContact
import org.opencovidtrace.octrace.data.BtContactHealth
import org.opencovidtrace.octrace.data.BtEncounter
import org.opencovidtrace.octrace.data.LogTableValue


@Database(
    entities = [LogTableValue::class,
        BtContactHealth::class,
        BtContact::class,
        BtEncounter::class],
    version = 3
)

@TypeConverters(DatabaseConverters::class)
abstract class Database : RoomDatabase() {

    abstract fun appDao(): OctraceDao

}
