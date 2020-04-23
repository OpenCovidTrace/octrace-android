package org.opencovidtrace.octrace.data

import android.bluetooth.BluetoothDevice
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

data class ConnectedDevice(var device: BluetoothDevice, var receiveInfo: String? = null)

@Entity(tableName = "log_table")
data class LogTableValue(
    val event: String? = null, val additionalInfo: String? = null, val time: Calendar = Calendar.getInstance(),
    @PrimaryKey(autoGenerate = true) var id: Int? = null
) {

    fun getLogValue() = "$event: $additionalInfo"
}