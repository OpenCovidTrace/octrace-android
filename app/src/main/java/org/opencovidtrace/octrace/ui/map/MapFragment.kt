package org.opencovidtrace.octrace.ui.map

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.fragment_map.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.data.ContactCoord
import org.opencovidtrace.octrace.data.LocationIndex
import org.opencovidtrace.octrace.data.UpdateUserTracksEvent
import org.opencovidtrace.octrace.di.api.ApiClientProvider
import org.opencovidtrace.octrace.ext.ifAllNotNull
import org.opencovidtrace.octrace.ext.text.dateFullFormat
import org.opencovidtrace.octrace.ext.ui.toast
import org.opencovidtrace.octrace.location.LocationUpdateManager
import org.opencovidtrace.octrace.storage.*
import org.opencovidtrace.octrace.ui.map.logs.LogsFragment
import org.opencovidtrace.octrace.ui.map.qrcode.QrCodeFragment
import org.opencovidtrace.octrace.utils.CryptoUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        const val userPolylineColor: Int = 0xFF0000ff.toInt()
        const val sickPolylineColor: Int = 0xFFF57F17.toInt()
    }

    private val apiClient by ApiClientProvider()
    private lateinit var mapViewModel: MapViewModel
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    private var mkContactPoints: HashMap<Marker, QrContact> = hashMapOf()
    var userPolylines = mutableListOf<Polyline>()
    var sickPolylines = mutableListOf<Polyline>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mapViewModel =
            ViewModelProvider(this).get(MapViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_map, container, false)

        mapView = root.findViewById(R.id.map)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync(this)
        return root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
        logsImageButton.setOnClickListener { showLogs() }
        recordContactButton.setOnClickListener { showQrCode() }
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map
        LocationUpdateManager.registerCallback { location ->
            loadTracks(location)
            loadDiagnosticKeys(location)

            activity?.runOnUiThread {
                map?.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(14f)
                            .build()
                    )
                )
                map?.isMyLocationEnabled = true
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUserTracksEvent(event: UpdateUserTracksEvent) {
        updateUserTracks()
    }

    private fun updateUserTracks() {
        println("Updating user tracks...")

        val polylines = makePolylines(TrackingManager.getTrackingData())

        println("Got ${polylines.size} user polylines.")

        userPolylines.forEach { it.remove() }
        userPolylines.clear()
        polylines.forEach {
            googleMap?.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .addAll(it)
            )?.let { polyline ->
                polyline.color = userPolylineColor
                userPolylines.add(polyline)
            }

        }

    }

    private fun updateExtTracks() {
        println("Updating external tracks...")

        val sickPolylines: MutableList<List<LatLng>> = mutableListOf()

        TracksManager.getTracks().forEach { track ->
            val trackPolylines = makePolylines(track.points)
            sickPolylines.addAll(trackPolylines)
        }

        println("Got ${sickPolylines.size} sick polylines.")

        val now = System.currentTimeMillis()

        this.sickPolylines.forEach { it.remove() }
        this.sickPolylines.clear()
        sickPolylines.forEach {
            googleMap?.addPolyline(
                PolylineOptions()
                    .clickable(true)
                    .addAll(it)
            )?.let { polyline ->
                polyline.color = sickPolylineColor
                this.sickPolylines.add(polyline)
            }
        }

        val renderTime = System.currentTimeMillis() - now

        print("Rendered ${sickPolylines.size} sick polylines in $renderTime ms.")

        // So that user tracks are always above
        updateUserTracks()
    }

    private fun makePolylines(points: List<TrackingPoint>): List<List<LatLng>> {
        val polylines: MutableList<List<LatLng>> = mutableListOf()
        var lastPolyline = mutableListOf<LatLng>()
        var lastTimestamp = 0L

        fun addPolyline() {
            if (lastPolyline.size == 1) {
                // Each polyline should have at least 2 points
                lastPolyline.add(lastPolyline.first())
            }

            polylines.add(lastPolyline)
        }

        points.forEach { point ->
            val timestamp = point.tst
            val coordinate = point.coordinate()

            when {
                lastTimestamp == 0L -> {
                    lastPolyline = arrayListOf(coordinate)
                }
                timestamp - lastTimestamp > TrackingManager.trackingIntervalMs * 2 -> {
                    addPolyline()

                    lastPolyline = arrayListOf(coordinate)
                }
                else -> {
                    lastPolyline.add(coordinate)
                }
            }

            lastTimestamp = timestamp
        }

        addPolyline()

        return polylines
    }

    fun updateContacts() {

        mkContactPoints.keys.forEach { it.remove() }
        mkContactPoints.clear()

        QrContactsManager.getContacts().forEach { contact ->
            ifAllNotNull(contact.metaData, contact.metaData?.coord) { metaData, coord ->
                googleMap?.addMarker(
                    MarkerOptions().position(coord.coordinate())
                        .title(getString(R.string.contact_at_date, metaData.date.dateFullFormat()))
                )?.let {
                    mkContactPoints[it] = contact
                }
            }
        }

    }

    fun goToContact(coord: ContactCoord) {
        goToLocation(LatLng(coord.lat, coord.lng))
    }

    private fun goToLocation(latLng: LatLng) {
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
    }

    private fun showLogs() {
        val dialog = LogsFragment()
        dialog.show(childFragmentManager, dialog.tag)
    }

    private fun showQrCode() {
        val dialog = QrCodeFragment()
        dialog.show(childFragmentManager, dialog.tag)
    }

    override fun onResume() {
        super.onResume()
        updateUserTracks()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()

        mapView.onPause()
    }

    override fun onStart() {
        super.onStart()

        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()

        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()

        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        mapView.onDestroy()
        super.onDestroy()
    }

    private fun loadTracks(location: Location) {
        val index = LocationIndex(location)
        val lastUpdateTimestamp = LocationIndexManager.getTracksIndex()[index] ?: 0
        val border = LocationBordersManager.LocationBorder.fetchLocationBorderByIndex(index)

        apiClient.fetchTracks(
            lastUpdateTimestamp,
            border.minLat,
            border.maxLat,
            border.minLng,
            border.maxLng
        ).enqueue(object : Callback<TracksData> {

            override fun onResponse(call: Call<TracksData>, response: Response<TracksData>) {
                response.body()?.tracks?.let { tracks ->
                    LocationIndexManager.updateTracksIndex(index)

                    if (tracks.isEmpty()) {
                        return
                    }

                    val latestSecretDailyKeys = CryptoUtil.getLatestSecretDailyKeys()

                    val tracksFiltered = tracks.filter { !latestSecretDailyKeys.contains(it.key) }

                    Log.i(
                        "TRACKS",
                        "Got ${tracksFiltered.size} new tracks since $lastUpdateTimestamp for ${border}."
                    )

                    if (tracksFiltered.isEmpty()) {
                        return
                    }

                    TracksManager.addTracks(tracksFiltered)
                    updateExtTracks()
                }
            }

            override fun onFailure(call: Call<TracksData>, t: Throwable) {
                Log.e("TRACKS", "ERROR: ${t.message}", t)
            }
        })
    }

    private fun loadDiagnosticKeys(location: Location) {
        val index = LocationIndex(location)
        val lastUpdateTimestamp = LocationIndexManager.getKeysIndex()[index] ?: 0
        val border = LocationBordersManager.LocationBorder.fetchLocationBorderByIndex(index)

        apiClient.fetchKeys(
            lastUpdateTimestamp,
            border.minLat,
            border.maxLat,
            border.minLng,
            border.maxLng
        ).enqueue(object : Callback<KeysData> {

            override fun onResponse(call: Call<KeysData>, response: Response<KeysData>) {
                response.body()?.let { data ->
                    LocationIndexManager.updateKeysIndex(index)

                    if (data.keys.isNullOrEmpty()) {
                        return
                    }

                    val qrContacts = QrContactsManager.matchContacts(data)
                    val btContacts = BtContactsManager.matchContacts(data)

                    val hasQrExposure = qrContacts.first
                    val hasBtExposure = btContacts.first

                    if (hasQrExposure || hasBtExposure) {
                        showExposedNotification()
                    }

                    qrContacts.second?.let {
                        goToContact(it)
                        updateContacts()
                    } ?: btContacts.second?.let {
                        goToContact(it)
                        updateContacts()
                    }

                }
            }

            override fun onFailure(call: Call<KeysData>, t: Throwable) {
                Log.e("TRACKS", "ERROR: ${t.message}", t)
            }
        })
    }

    private fun showExposedNotification() {
        toast(R.string.exposed_contact_message)
    }

}
