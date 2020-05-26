package org.opencovidtrace.octrace.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import org.opencovidtrace.octrace.ext.text.dateFullFormat
import java.util.*

const val ADV_TAG = "ADV"
const val SCAN_TAG = "SCAN"

interface LogTableValue {
    fun getLog(): String
}

@Entity(tableName = "bt_log_table")
data class BtLogTableValue(
    val tag: String,
    val text: String,
    val time: Date = Date(),
    @PrimaryKey(autoGenerate = true) var id: Int? = null
) : LogTableValue {
    override fun getLog() = "[${time.dateFullFormat()}] <$tag> $text"
}

@Entity(tableName = "dp3t_log_table")
data class Dp3tLogTableValue(
    val text: String,
    val time: Date = Date(),
    @PrimaryKey(autoGenerate = true) var id: Int? = null
) : LogTableValue {
    override fun getLog() = "[${time.dateFullFormat()}] $text"
}


data class ContactMetaData(val coord: ContactCoord?, val date: Date)

data class ContactCoord(val lat: Double, val lng: Double, val accuracy: Int) {
    fun coordinate(): LatLng = LatLng(lat, lng)
}
