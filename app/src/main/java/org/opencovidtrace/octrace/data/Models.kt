package org.opencovidtrace.octrace.data

import android.bluetooth.BluetoothDevice
import android.location.Location
import androidx.room.*
import java.util.*

data class ConnectedDevice(var device: BluetoothDevice, var receiveInfo: String? = null)

@Entity(tableName = "log_table")
data class LogTableValue(
    val event: String? = null,
    val additionalInfo: String? = null,
    val time: Calendar = Calendar.getInstance(),
    @PrimaryKey(autoGenerate = true) var id: Int? = null
) {
    fun getLogValue() = "$event: $additionalInfo"
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
    constructor(rssi: Int, location: Location):this(rssi,location.latitude,location.longitude,location.accuracy)
}

