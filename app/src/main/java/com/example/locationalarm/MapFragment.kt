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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import android.R.attr.radius
import com.google.android.gms.maps.model.CircleOptions




class MapFragment : Fragment(), OnMapReadyCallback{
    private var SYDNEY: LatLng? = null
    private var DESTINATION: LatLng? = null
    val ZOOM_LEVEL = 13f
    private lateinit var mMap: GoogleMap
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
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney")).setDraggable(true)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
        mMap.addCircle(
            CircleOptions()
                .center(sydney)
                .radius(10000*radius.toDouble())
                .strokeWidth(10f)
                .strokeColor(0x550000FF)
                .fillColor(0x550000FF)
        )
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)
        val mapFragment =  childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)




    }

}
