package org.opencovidtrace.octrace.service

import android.Manifest
import android.app.*
import android.app.NotificationManager.IMPORTANCE_LOW
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.opencovidtrace.octrace.MainActivity
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.bluetooth.DeviceManager
import org.opencovidtrace.octrace.data.ConnectedDevice
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
    }

    private val binder: IBinder = LocalBinder()

    private var changingConfiguration = false
    private var notificationManager: NotificationManager? = null

    private var scanWorkTimer: Timer? = null
    private var scanPauseTimer: Timer? = null

    /* Collection of devices found */
    private val foundedDevices = mutableSetOf<ConnectedDevice>()

    private val deviceManager by BluetoothManagerProvider()
    private var tickReceiver: TickReceiver? = null


    inner class TickReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (serviceIsRunningInForeground(applicationContext)) {
                notificationManager?.notify(NOTIFICATION_ID, getNotification())
            }
        }
    }

    override fun onCreate() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
            notificationManager = this
            // Android O requires a Notification Channel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name: CharSequence = getString(R.string.app_name)
                val channel = NotificationChannel(SILENT_CHANNEL_ID, name, IMPORTANCE_LOW)
                createNotificationChannel(channel)
            }
        }
        deviceManager.setDeviceStatusListener(object : DeviceManager.DeviceStatusListener {
            override fun onDataReceived(device: BluetoothDevice, bytes: ByteArray) {
                val bytesString = bytes.contentToString()
                foundedDevices.firstOrNull { it.device.address == device.address }?.let {
                    it.receiveInfo=bytesString
                }
            }

            override fun onServiceNotFound(device: BluetoothDevice) {
                foundedDevices.firstOrNull { it.device.address == device.address }?.let {
                    it.receiveInfo="service not found"
                }
            }

        })
        tickReceiver = TickReceiver()
        registerReceiver(tickReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
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
        changingConfiguration = true
    }

    override fun onBind(intent: Intent): IBinder? {
        insertLogs("onBind", TAG)
        stopForeground(true)
        changingConfiguration = false
        return binder
    }

    override fun onRebind(intent: Intent) {
        insertLogs("onCreate", "onRebind")
        stopForeground(true)
        changingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        insertLogs("onCreate", "onUnbind")
        if (!changingConfiguration) {
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
        unregisterReceiver(tickReceiver)
        insertLogs("onDestroy", TAG)
    }


    fun requestBleUpdates() {
        if (hasPermissions()) {
            if (!serviceIsStarted(applicationContext))
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
        if (!serviceIsStarted(applicationContext))
            startService(Intent(applicationContext, BleUpdatesService::class.java))
        try {
            deviceManager.startAdvertising()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onBleDeviceFound(result: ScanResult) {
        if (foundedDevices.firstOrNull { it.device.address == result.device.address } == null) {
            if (deviceManager.connectDevice(result, ::onBleDeviceConnect)) {
                foundedDevices.add(ConnectedDevice(result.device))
                insertLogs("DEVICE FOUND", result.toString())
            }
        }
    }

    private fun onBleDeviceConnect(device: BluetoothDevice, result: Boolean) {
        if (!result)
            foundedDevices.firstOrNull { it.device.address == device.address }?.let {
                foundedDevices.remove(it)
            }
    }

    fun stopBleService(){
        deviceManager.stopSearchDevices()
        deviceManager.closeConnection()
        deviceManager.stopServer()
        deviceManager.stopAdvertising()
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
                .setContentTitle("TEST TITLE")
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

    inner class LocalBinder : Binder() {
        val service: BleUpdatesService get() = this@BleUpdatesService
    }

    private fun serviceIsStarted(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.started) {
                    return true
                }
            }
        }
        return false
    }

    fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
    }


}