package org.opencovidtrace.octrace.ui.map

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
import org.opencovidtrace.octrace.location.LocationUpdateManager
import org.opencovidtrace.octrace.ui.map.logs.LogsFragment
import org.opencovidtrace.octrace.ui.map.qrcode.QrCodeFragment

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapViewModel: MapViewModel
    private lateinit var mapView: MapView

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
}
