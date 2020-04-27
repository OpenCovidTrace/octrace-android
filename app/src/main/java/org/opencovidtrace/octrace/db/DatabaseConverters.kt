package org.opencovidtrace.octrace.db

import androidx.room.TypeConverter
import java.util.*

class DatabaseConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Calendar? = value?.let { value ->
        GregorianCalendar().also { calendar ->
            calendar.timeInMillis = value
        }
    }

    @TypeConverter
    fun toTimestamp(timestamp: Calendar?): Long? = timestamp?.timeInMillis

}