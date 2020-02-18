package com.example.locationalarm

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
import android.graphics.Color
import android.location.Geocoder
import android.os.Handler
import java.util.*
import androidx.core.os.HandlerCompat.postDelayed
import android.os.SystemClock
import android.view.animation.BounceInterpolator
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.*


class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var geocoder: Geocoder

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
            sydney = it
            jumpingMarker(it, marker)

        }
        mMap.setOnMarkerClickListener {

            jumpingMarker(sydney, it)
            return@setOnMarkerClickListener true
        }
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
                    handler.postDelayed(this, 16)
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


    }

}
