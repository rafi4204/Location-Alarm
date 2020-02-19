package com.example.locationalarm

import android.Manifest
import android.app.Activity
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import android.R.attr.radius
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import java.util.*
import androidx.core.os.HandlerCompat.postDelayed
import android.os.SystemClock
import android.view.animation.BounceInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var geocoder: Geocoder

    private var SYDNEY: LatLng? = null
    private var DESTINATION: LatLng? = null
    val ZOOM_LEVEL = 13f
    lateinit var mLastLocation: Location
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var mLocationPermissionGranted = false
    val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101

    companion object {
        fun newInstance() = MapFragment()
    }

    private lateinit var viewModel: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        return inflater.inflate(R.layout.map_fragment, container, false)

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        var sydney = LatLng(-34.0, 151.0)
        val marker = mMap.addMarker(
            MarkerOptions().position(sydney).title(
                geocoder.getFromLocation(
                    sydney.latitude,
                    sydney.longitude,
                    1
                )[0].locality
            )
        )
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sydney, 14f))
        val circle = mMap.addCircle(
            CircleOptions()
                .center(sydney)
                .radius(1000.0)
                .strokeColor(Color.RED)
                .fillColor(Color.BLUE)
        )
        mMap.setOnMapClickListener {
            marker.isVisible = false
            marker.position = it
            marker.title = geocoder.getFromLocation(it.latitude, it.longitude, 1)[0].locality
            circle.center = it
            marker.isVisible = true
            marker.isDraggable = true
            sydney = it
            jumpingMarker(it, marker)

        }
        mMap.setOnMarkerClickListener {

            jumpingMarker(sydney, it)
            return@setOnMarkerClickListener true
        }

        // jumpingMarker(sydney, marker)


    }

    private fun jumpingMarker(latLng: LatLng, marker: Marker) {

        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val proj = mMap.projection
        val startPoint = proj.toScreenLocation(latLng)
        startPoint.offset(0, -100)
        val startLatLng = proj.fromScreenLocation(startPoint)
        val duration: Long = 1500
        val interpolator = BounceInterpolator()
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val lng = t * latLng.longitude + (1 - t) * startLatLng.longitude
                val lat = t * latLng.latitude + (1 - t) * startLatLng.latitude
                marker.setPosition(LatLng(lat, lng))
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 1)
                }
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        geocoder = Geocoder(context, Locale.getDefault())
        // var i=0
        /*GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {   Log.d("2",i++.toString()) } // Get from IO context
            // Back on main thread
            //  Log.d("2",i++.toString())
            //    jumpingMarker(sydney, marker)

        }
*/

        mFusedLocationProviderClient = activity?.let {
            LocationServices.getFusedLocationProviderClient(
                it
            )
        }!!
        getLocationPermission()

    }

    private fun getLocationPermission() {
        if (context?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationPermissionGranted = true
        } else {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }

        }
        getDeviceLocation()
    }

    private fun getDeviceLocation() {
        try {
            if (mLocationPermissionGranted) {
                val locationResult = mFusedLocationProviderClient.lastLocation

                locationResult?.addOnSuccessListener { location: Location? ->
                    Log.d("2", location?.latitude.toString())
                    Log.d("2", location?.longitude.toString())


                    if (location != null) {
                        SYDNEY = LatLng(location.latitude, location.longitude)
                    }


                    /* val mapFragment: SupportMapFragment? =
                         supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
                     mapFragment?.getMapAsync(this)*/
                }


            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                    mLocationPermissionGranted = true
                    getDeviceLocation()
                } else {
                    Toast.makeText(context, "Permission Denied!!Are you ", Toast.LENGTH_SHORT).show()
                }
                return
            }

        }
    }


}
