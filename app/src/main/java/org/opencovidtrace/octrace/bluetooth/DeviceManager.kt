package org.opencovidtrace.octrace.bluetooth

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import org.opencovidtrace.octrace.data.BtEncounter
import org.opencovidtrace.octrace.data.Enums
import org.opencovidtrace.octrace.ext.data.insertLogs
import org.opencovidtrace.octrace.location.LocationUpdateManager
import org.opencovidtrace.octrace.storage.BtContactsManager
import org.opencovidtrace.octrace.utils.SecurityUtil
import java.util.*


class DeviceManager(private val context: Context) {

    companion object {
        private val TAG = DeviceManager::class.java.simpleName
        val SERVICE_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9f")
        val MAIN_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    }

    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    // To provide bluetooth communication
    private var bluetoothGatt: BluetoothGatt? = null

    private var scanCallback: ScanCallback? = null
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var scanActive = false
    private var advertisingActive = false

    private var deviceStatusListener: DeviceStatusListener? = null

    /**
     * Add listener to receive scanned data
     *
     * @see DeviceStatusListener
     */
    fun setDeviceStatusListener(listener: DeviceStatusListener?) {
        this.deviceStatusListener = listener
    }

    /**
     * Check is Bluetooth LE is available and is it turned on
     *
     * @return current state of Bluetooth scanner
     * @see Enums
     */
    fun checkBluetooth(): Enums {
        val hasSupportLe = context.packageManager
            ?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
            ?: false

        return if (bluetoothAdapter == null || !hasSupportLe) {
            Enums.NOT_FOUND
        } else if (!bluetoothAdapter?.isEnabled!!) {
            Enums.DISABLED
        } else {
            Enums.ENABLED
        }
    }

    /**
     * Start searching Bluetooth LE devices according to the selected device type
     * and return one by one found devices via devicesCallback
     *
     * @param devicesCallback a callback for found devices
     *
     */
    fun startSearchDevices(
        devicesCallback: (ScanResult) -> Unit,
        fromTimer: Boolean = false
    ) {
        if (scanActive)// && bluetoothAdapter?.isDiscovering == true
            return
        if (!fromTimer)
            stopSearchDevices()

        val deviceFilter = ScanFilter.Builder()
            .apply { setServiceUuid(ParcelUuid(SERVICE_UUID)) }
            .build()

        val bluetoothSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val scanRecord = result.scanRecord
                if (scanRecord != null) {
                    devicesCallback(result)
                }
            }
        }

        bluetoothAdapter?.bluetoothLeScanner?.startScan(
            mutableListOf(deviceFilter),
            bluetoothSettings,
            scanCallback
        )
        scanActive = true
        insertLogs("Start Scan", TAG)
    }

    /**
     * Stop Bluetooth LE scanning process
     */
    fun stopSearchDevices() {
        bluetoothAdapter?.isDiscovering?.let {
            bluetoothAdapter?.cancelDiscovery()
        }
        scanCallback?.let { bluetoothAdapter?.bluetoothLeScanner?.stopScan(it) }
        scanCallback = null
        insertLogs("Stop Scan", TAG)
        scanActive = false
    }

    /**
     * Arrange connection to the selected device, and read characteristics of the identified device type
     *
     * @param device instance of BluetoothDevice that was received during scanning process
     *
     */
    fun connectDevice(
        scanResult: ScanResult,
        deviceConnectCallback: (BluetoothDevice, Boolean) -> Unit
    ): Boolean {
        val device=scanResult.device
        if (isDeviceConnected()) {
            return false
        }

        bluetoothGatt = device.connectGatt(
            context,
            false,
            object : BluetoothGattCallback() {

                override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                    super.onMtuChanged(gatt, mtu, status)
                    insertLogs("Mtu Changed", "$mtu status $status")
                    gatt?.discoverServices()
                }

                override fun onConnectionStateChange(
                    gatt: BluetoothGatt,
                    status: Int,
                    newState: Int
                ) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            insertLogs("Device Connected", device.address)
                            deviceConnectCallback(device, true)
                            val mtu = 32 + 3 // Maximum allowed 517 - 3 bytes do BLE
                            bluetoothGatt?.requestMtu(mtu)

                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            insertLogs("Device Disconnected", device.address)
                            deviceConnectCallback(device, false)
                            closeConnection()
                        }

                    }
                    when (status) {
                        BluetoothGatt.GATT_FAILURE -> {
                            insertLogs("Device connection failure", device.address)
                            deviceConnectCallback(device, false)
                            closeConnection()
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    insertLogs("Services Discovered", device.address)
                    var hasServiceAndCharacteristic = false
                    val service = gatt.getService(SERVICE_UUID)
                    if (service != null) {
                        val characteristic =
                            service.getCharacteristic(MAIN_CHARACTERISTIC_UUID)
                        characteristic?.let {
                            bluetoothGatt?.readCharacteristic(it)
                            hasServiceAndCharacteristic = true
                        }

                    }
                    if (!hasServiceAndCharacteristic) {
                        deviceStatusListener?.onServiceNotFound(device)
                        closeConnection()
                    }
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        handleCharacteristics(scanResult, characteristic)
                    }
                }

            })

        return true
    }

    /**
     * Close connection with earlier connected device
     */
    fun closeConnection() {
        insertLogs("Close Connection", bluetoothGatt?.device?.address ?: "")
        bluetoothGatt?.close()
        bluetoothGatt = null
    }


    private fun isDeviceConnected() = bluetoothGatt != null


    private fun handleCharacteristics(
        scanResult: ScanResult,
        characteristic: BluetoothGattCharacteristic
    ) {
        insertLogs(
            "Success Characteristic Read ",
            characteristic.value?.contentToString() ?: "is empty"
        )
        val base64 = encodeToString(characteristic.value, DEFAULT)
        deviceStatusListener?.onDataReceived(scanResult.device, characteristic.value)
        val location= LocationUpdateManager.getLastLocation()
        location?.let {
            BtContactsManager.addContact(base64, BtEncounter(scanResult.rssi,location))
        }

        closeConnection()
    }


    interface DeviceStatusListener {
        fun onDataReceived(device: BluetoothDevice, bytes: ByteArray)
        fun onServiceNotFound(device: BluetoothDevice)
    }


    /********************************************
     ******************SERVICE*******************
     ********************************************/

    /**
     * Begin advertising over Bluetooth that this device is connectable
     */
    fun startAdvertising(): Boolean {
        if (advertisingActive)
            return true
        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            insertLogs("Advertisement not supported", TAG)
            return false
        }

        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothManager.adapter.bluetoothLeAdvertiser

        bluetoothLeAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
            insertLogs("Start Advertising", SERVICE_UUID.toString())
            advertisingActive = true
            return true
        } ?: return false
    }

    /**
     * Stop Bluetooth advertisements.
     */
    fun stopAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            bluetoothManager.adapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.let {
            it.stopAdvertising(advertiseCallback)
        } ?: insertLogs("Failed to create advertiser", TAG)
        advertisingActive = false
        insertLogs("Stop Advertising", TAG)
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            insertLogs("Start Advertising Success ", TAG)
            if (!startBleServer())
                insertLogs("Unable to create GATT server", TAG)
        }

        override fun onStartFailure(errorCode: Int) {
            insertLogs("Start Advertising Failure ", "errorCode $errorCode")
        }
    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     */
    private fun startBleServer(): Boolean {
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        return bluetoothGattServer?.addService(createService()) ?: false
    }

    private fun createService(): BluetoothGattService {
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val characteristic = BluetoothGattCharacteristic(
            MAIN_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        service.addCharacteristic(characteristic)

        return service
    }

    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                insertLogs("Connection State Change state connected", device.address)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                insertLogs("Connection State Change state disconnected", device.address)
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice, requestId: Int, offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            val now = System.currentTimeMillis()
            when (characteristic.uuid) {
                MAIN_CHARACTERISTIC_UUID -> {
                    insertLogs("Read Server Characteristic", characteristic.uuid.toString())
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        SecurityUtil.getRollingId()
                    )
                }
                else -> {
                    // Invalid characteristic
                    insertLogs("Invalid Characteristic Read", characteristic.uuid.toString())
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null
                    )
                }
            }
        }


        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            super.onExecuteWrite(device, requestId, execute)
            insertLogs("Execute Write", device?.address ?: "")
        }

    }

    fun stopServer() {
        insertLogs("Stop gatt server", "")
        bluetoothGattServer?.close()
    }

}