package org.opencovidtrace.octrace.ui.map

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.fragment_map.*
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.data.LocationIndex
import org.opencovidtrace.octrace.data.TracksData
import org.opencovidtrace.octrace.di.api.ApiClientProvider
import org.opencovidtrace.octrace.location.LocationUpdateManager
import org.opencovidtrace.octrace.storage.LocationBordersManager
import org.opencovidtrace.octrace.storage.LocationIndexManager
import org.opencovidtrace.octrace.storage.TracksManager
import org.opencovidtrace.octrace.ui.map.logs.LogsFragment
import org.opencovidtrace.octrace.ui.map.qrcode.QrCodeFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapViewModel: MapViewModel
    private lateinit var mapView: MapView
    private val apiClient by ApiClientProvider()

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
        logsImageButton.setOnClickListener { showLogs() }
        recordContactButton.setOnClickListener { showQrCode() }
    }

    override fun onMapReady(map: GoogleMap?) {
        LocationUpdateManager.registerCallback { location ->
            loadTracks(location)
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
        super.onDestroy()

        mapView.onDestroy()
    }

    private fun loadTracks(location: Location) {
        val index = LocationIndex(location)
        val lastUpdateTimestamp = LocationIndexManager.getKeysIndex()[index] ?: 0
        val border = LocationBordersManager.LocationBorder.fetchLocationBorderByIndex(index)

        apiClient.fetchTracks(
            lastUpdateTimestamp,
            border.minLat,
            border.maxLat,
            border.minLng,
            border.maxLng
        ).enqueue(object : Callback<TracksData> {

            override fun onResponse(call: Call<TracksData>, response: Response<TracksData>) {
                response.body()?.tracks?.let { TracksManager.addTracks(it) }
            }

            override fun onFailure(call: Call<TracksData>, t: Throwable) {
                println("ERROR: ${t.message}")
            }

        })
    }
}
