package org.opencovidtrace.octrace.service

import android.Manifest
import android.app.*
import android.app.NotificationManager.IMPORTANCE_LOW
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.opencovidtrace.octrace.MainActivity
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.data.ConnectedDevice
import org.opencovidtrace.octrace.data.Enums
import org.opencovidtrace.octrace.di.BluetoothManagerProvider
import org.opencovidtrace.octrace.ext.access.isNotGranted
import org.opencovidtrace.octrace.ext.data.insertLogs
import org.opencovidtrace.octrace.ext.text.dateTimeFormat
import java.util.*


class BleUpdatesService : Service() {

    companion object {
        private val TAG = BleUpdatesService::class.java.simpleName

        private const val SILENT_CHANNEL_ID = "silent_channel_ble"
        private const val NOTIFICATION_ID = 7856234

        private var isRunningInInForegroundService = false
    }

    private val binder: IBinder = LocalBinder()

    private var changingConfiguration = false
    private var notificationManager: NotificationManager? = null
    private var scanWorkTimer: Timer? = null
    private var scanPauseTimer: Timer? = null

    /* Collection of devices found */
    private val foundDevices = mutableSetOf<ConnectedDevice>()
    private val deviceManager by BluetoothManagerProvider()

    private var bluetoothState: Int = -1

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            println("BluetoothReceiver onReceive")
            val action = intent.action
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                bluetoothState =
                    intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                println("BluetoothReceiver onReceive $bluetoothState")
                if (serviceIsRunningInForeground()) {
                    notificationManager?.notify(NOTIFICATION_ID, getNotification())
                }
                when (bluetoothState) {
                    BluetoothAdapter.STATE_OFF -> stopBleService()
                    BluetoothAdapter.STATE_ON -> {
                        startAdvertising()
                        requestBleUpdates()
                    }
                }
            }
        }
    }
    private val tickReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (serviceIsRunningInForeground()) {
                notificationManager?.notify(NOTIFICATION_ID, getNotification())
            }
        }
    }


    override fun onCreate() {
        insertLogs("onCreate", TAG)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
            notificationManager = this
            // Android O requires a Notification Channel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name: CharSequence = getString(R.string.app_name)
                val channel = NotificationChannel(SILENT_CHANNEL_ID, name, IMPORTANCE_LOW)
                createNotificationChannel(channel)
            }
        }
        bluetoothState =
            if (deviceManager.checkBluetooth() == Enums.ENABLED) BluetoothAdapter.STATE_ON
            else BluetoothAdapter.STATE_OFF
        registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    private fun initScanWorkTimer() {
        if (scanWorkTimer == null) {
            scanWorkTimer = Timer()
            val timerAlarmInterval: Long = (30 * 1000).toLong()//30 second
            scanWorkTimer?.scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        scanWorkTimer?.cancel()
                        scanWorkTimer = null
                        deviceManager.stopSearchDevices()
                        initScanPauseTimer()

                    }
                },
                timerAlarmInterval, timerAlarmInterval
            )
        }
    }

    private fun initScanPauseTimer() {
        if (scanPauseTimer == null) {
            scanPauseTimer = Timer()
            val timerAlarmInterval: Long = (10 * 1000).toLong()//10 sec
            scanPauseTimer?.scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        scanPauseTimer?.cancel()
                        scanPauseTimer = null
                        deviceManager.startSearchDevices(::onBleDeviceFound, true)
                        initScanWorkTimer()
                    }
                }, timerAlarmInterval, timerAlarmInterval
            )
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        insertLogs("onStartCommand", TAG)
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        insertLogs("onConfigurationChanged", TAG)
        changingConfiguration = true
    }

    override fun onBind(intent: Intent): IBinder? {
        insertLogs("onBind", TAG)
        stopForeground(true)
        isRunningInInForegroundService = false
        changingConfiguration = false
        return binder
    }

    override fun onRebind(intent: Intent) {
        insertLogs("onRebind", TAG)
        stopForeground(true)
        isRunningInInForegroundService = false
        changingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        insertLogs("onUnbind", TAG)
        if (!changingConfiguration) {
            isRunningInInForegroundService = true
            startForeground(NOTIFICATION_ID, getNotification())
        }
        return true // Ensures onRebind() is called when a client re-binds.
    }

    private fun hasPermissions(): Boolean {
        val notGrantedPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).filter { it.isNotGranted() }
        return notGrantedPermissions.isEmpty()
    }


    override fun onDestroy() {
        isRunningInInForegroundService = false
        unregisterReceiver(tickReceiver)
        unregisterReceiver(bluetoothReceiver)
        insertLogs("onDestroy", TAG)
    }


    fun requestBleUpdates() {
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val gpsEnabled =
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ?: false
        if (hasPermissions() && gpsEnabled) {
            startService(Intent(applicationContext, BleUpdatesService::class.java))
            try {
                deviceManager.startSearchDevices(::onBleDeviceFound)
                initScanWorkTimer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startAdvertising() {
        startService(Intent(applicationContext, BleUpdatesService::class.java))
        try {
            deviceManager.startAdvertising()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onBleDeviceFound(result: ScanResult) {
        val device = ConnectedDevice(result.device, result.rssi)
        if (foundDevices.firstOrNull { it == device } == null) {
            if (deviceManager.connectDevice(result, ::onBleDeviceConnect)) {
                foundDevices.add(device)
            }
        } else {
            insertLogs("Not connecting to ${result.device.address}, duplicate RSSI value.", TAG)
        }
    }

    private fun onBleDeviceConnect(device: BluetoothDevice, result: Boolean) {
        if (!result)
            foundDevices.firstOrNull { it.device.address == device.address }?.let {
                foundDevices.remove(it)
            }
    }

    fun stopBleService(needStopSelf: Boolean = false) {
        deviceManager.stopSearchDevices()
        deviceManager.closeConnection()
        deviceManager.stopServer()
        deviceManager.stopAdvertising()
        scanWorkTimer?.cancel()
        scanWorkTimer = null
        scanPauseTimer?.cancel()
        scanPauseTimer = null
        if (needStopSelf)
            stopSelf()
    }


    private fun getNotification(): Notification? {
        val text: CharSequence = Calendar.getInstance().dateTimeFormat()
        val intent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, SILENT_CHANNEL_ID)
                .setContentText(text)
                .setContentTitle(getBluetoothState())
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
        builder.priority =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) IMPORTANCE_LOW
            else Notification.PRIORITY_LOW

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_SERVICE)
        }
        return builder.build()
    }

    private fun getBluetoothState(): String {
        return getString(
            when (bluetoothState) {
                BluetoothAdapter.STATE_OFF -> R.string.bluetooth_off
                BluetoothAdapter.STATE_TURNING_OFF -> R.string.turning_bluetooth_off
                BluetoothAdapter.STATE_ON -> R.string.bluetooth_on
                BluetoothAdapter.STATE_TURNING_ON -> R.string.turning_bluetooth_on
                else -> R.string.bluetooth_unknown_state
            }
        )
    }

    inner class LocalBinder : Binder() {
        val service: BleUpdatesService get() = this@BleUpdatesService
    }


    fun serviceIsRunningInForeground(): Boolean {
        return isRunningInInForegroundService
    }

}