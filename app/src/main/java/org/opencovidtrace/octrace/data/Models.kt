package org.opencovidtrace.octrace.data

import android.bluetooth.BluetoothDevice
import android.location.Location
import androidx.room.*
import com.google.android.gms.maps.model.LatLng
import org.opencovidtrace.octrace.ext.text.dateFullFormat
import org.opencovidtrace.octrace.storage.LocationBordersManager
import org.opencovidtrace.octrace.utils.CryptoUtil
import java.util.*
import kotlin.math.roundToInt

const val ADV_TAG = "ADV"
const val SCAN_TAG = "SCAN"

data class ConnectedDevice(val device: BluetoothDevice, val rssi: Int)

@Entity(tableName = "log_table")
data class LogTableValue(
    val tag: String,
    val text: String,
    val time: Calendar = Calendar.getInstance(),
    @PrimaryKey(autoGenerate = true) var id: Int? = null
) {
    fun getLogValue() = text

    fun getTimeWithTag() = time.dateFullFormat() + " - <$tag>"
}


@Entity(
    tableName = "contact_health_table",
    indices = [Index(value = ["contact_id"], unique = true)]
)
data class BtContactHealth(
    @Embedded(prefix = "contact_") val contact: BtContact,
    var infected: Boolean = false,
    @PrimaryKey(autoGenerate = true) var id: Int? = null
)


@Entity(tableName = "contact_table")
data class BtContact(@PrimaryKey val id: String)


class ContactWithEncounters {
    @Embedded
    lateinit var contactHealth: BtContactHealth

    @Relation(
        parentColumn = "contact_id",
        entityColumn = "contactId"
    )
    lateinit var encounters: List<BtEncounter>
}


@Entity(tableName = "encounter_table")
data class BtEncounter(
    val rssi: Int,
    var lat: Double,
    var lng: Double,
    val accuracy: Float,
    val tst: Calendar = Calendar.getInstance(),
    var contactId: String? = null,
    @PrimaryKey(autoGenerate = true) var id: Int? = null
) {
    constructor(rssi: Int, location: Location) : this(
        rssi,
        location.latitude,
        location.longitude,
        location.accuracy
    )
}

class KeysData {
    var keys: MutableList<Key> = arrayListOf()
}

data class Key(
    val value: String,
    val day: Int,
    val border: LocationBordersManager.LocationBorder
)

data class ContactRequest(
    val token: String,
    val platform: String,
    val secret: String,
    val tst: Long
)

data class QrContact(val id: String, val lat: Double, val lng: Double, val tst: Long) {

    fun coordinate(): LatLng = LatLng(lat, lng)

    fun date(): Calendar = Calendar.getInstance().apply { timeInMillis = tst }

}

data class QrContactHealth(val contact: QrContact, var infected: Boolean = false)


data class TrackingPoint(val lat: Double, val lng: Double, val tst: Long) {

    constructor(location: Location) : this(
        location.latitude,
        location.longitude,
        System.currentTimeMillis()
    )

    constructor(latLng: LatLng) : this(
        latLng.latitude,
        latLng.longitude,
        System.currentTimeMillis()
    )

    fun coordinate(): LatLng = LatLng(lat, lng)

    fun dayNumber() = CryptoUtil.getDayNumber(tst)

}

data class Track(var points: MutableList<TrackingPoint>, val day: Int, val key: String)

data class TracksData(var tracks: List<Track>)

data class LocationIndex(val latIdx : Int, val lngIdx: Int){

   companion object{
       const val diff = 0.25 // ~ 25km
       const val precision = 10.0 // ~ 10km square side per index
   }

    constructor(location: Location) : this(
        latIdx= (location.latitude * precision).roundToInt(),
        lngIdx= (location.longitude * precision).roundToInt()
    )
}