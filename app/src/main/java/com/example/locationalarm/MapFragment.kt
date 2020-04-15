package com.example.locationalarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import java.util.*


class MapFragment : Fragment(), OnMapReadyCallback {

    private val REQUEST_CHECK_SETTINGS = 101
    private lateinit var mMap: GoogleMap
    private lateinit var geocoder: Geocoder
    var mHandler: Handler = Handler()
    private var SYDNEY: LatLng? = null
    private var DESTINATION: LatLng? = null
    val ZOOM_LEVEL = 13f
    lateinit var mLastLocation: Location
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var mLocationPermissionGranted = false
    val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101

    companion object {
        fun newInstance() = MapFragment()
        val CHANNEL_ID = "exampleServiceChannel"
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
        // SYDNEY = LatLng(-34.0, 151.0)
        val marker = mMap.addMarker(
            SYDNEY?.let {
                MarkerOptions().position(it).title(
                    geocoder.getFromLocation(
                        it.latitude,
                        it.longitude,
                        1
                    )[0].locality
                )
            }
        )
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 14f))
        val circle = mMap.addCircle(
            CircleOptions()
                .center(SYDNEY)
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
            SYDNEY = it
            jumpingMarker(it, marker)

        }
        mMap.setOnMarkerClickListener {

            SYDNEY?.let { it1 -> jumpingMarker(it1, it) }
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

                /*if (Looper.myLooper() == null)
                {
                    Looper.prepare()
                }

                handler.looper*/

                val elapsed = SystemClock.uptimeMillis() - start
                val t = interpolator.getInterpolation(elapsed.toFloat() / duration)
                val lng = t * latLng.longitude + (1 - t) * startLatLng.longitude
                val lat = t * latLng.latitude + (1 - t) * startLatLng.latitude
                marker.setPosition(LatLng(lat, lng))
                if (t < 1.0) {
                    //   Post again 16ms later.

                    handler.postDelayed(this, 1000)
                }
                // SYDNEY?.let { jumpingMarker(it,marker) }
                //  Looper.loop()
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)
        /* val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
         mapFragment?.getMapAsync(this)*/
        geocoder = Geocoder(context, Locale.getDefault())
        // var i=0
        /*GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {   Log.d("2",i++.toString()) } // Get from IO context
            // Back on main thread
            //  Log.d("2",i++.toString())
            //    jumpingMarker(sydney, marker)

        }
*/
        // val downloadIntent = Intent(context, MyIntentService::class.java)


        //context?.startService(downloadIntent)
        /* createNotificationChannel()
         val intent = Intent(activity, MyIntentService::class.java)
         activity?.startService(intent)*/

        mFusedLocationProviderClient = activity?.let {
            LocationServices.getFusedLocationProviderClient(
                it
            )
        }!!
        getLocationPermission()

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Example Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            /* val   manager = getSystemService(NotificationManager.class)
               manager.createNotificationChannel(serviceChannel)*/
            /*  val notificationManager: NotificationManager =
            getSystemService(context!!,NotificationManager.class) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)*/
            val notificationManager =
                activity?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
        }
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
            createLocationRequest()
        } else {
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }

        }
        //  getDeviceLocation()
    }


    fun createLocationRequest() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = locationRequest?.let {
            LocationSettingsRequest.Builder()
                .addLocationRequest(it)
        }
        val client = activity?.let { LocationServices.getSettingsClient(it) }
        val task: Task<LocationSettingsResponse>? = client?.checkLocationSettings(builder?.build())
        task?.addOnSuccessListener { locationSettingsResponse ->
            getDeviceLocation()
        }

        task?.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        activity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
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
                    val mapFragment =
                        childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
                    mapFragment?.getMapAsync(this)


                }


            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            REQUEST_CHECK_SETTINGS->getDeviceLocation()
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
                    Toast.makeText(context, "Permission Denied!!Are you ", Toast.LENGTH_SHORT)
                        .show()
                }
                return
            }

        }
    }


}
