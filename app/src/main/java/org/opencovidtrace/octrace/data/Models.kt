package org.opencovidtrace.octrace.data

import android.bluetooth.BluetoothDevice
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
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


@Entity(tableName = "contact_health_table")
data class BtContactHealth(@Embedded(prefix = "contact_") val contact: BtContact,
                           var infected: Boolean =false,
                           @PrimaryKey(autoGenerate = true) var id: Int? = null)


@Entity(tableName = "contact_table")
data class BtContact(@PrimaryKey val id: String,
                     @Embedded(prefix = "encounters_") var encounters: BtEncounter)

@Entity(tableName = "encounter_table")
data class BtEncounter(
    val rssi: Int,
    var lat: Double,
    var lng: Double,
    val accuracy: Int,
    val tst: Calendar = Calendar.getInstance(),
    @PrimaryKey(autoGenerate = true) var id: Int? = null
)

