package org.opencovidtrace.octrace.db

import androidx.room.TypeConverter
import java.util.*

class DatabaseConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let {
        Date(it)
    }

    @TypeConverter
    fun toTimestamp(date: Date?): Long? = date?.time

}
