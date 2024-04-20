package com.example.pzgeplocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import android.widget.ZoomControls
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var zoomControls: ZoomControls

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        zoomControls = findViewById(R.id.zoomControls)
        zoomControls.setOnZoomInClickListener { mMap.animateCamera(CameraUpdateFactory.zoomIn()) }
        zoomControls.setOnZoomOutClickListener { mMap.animateCamera(CameraUpdateFactory.zoomOut()) }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLocation = LatLng(it.latitude, it.longitude)
                    mMap.addMarker(MarkerOptions().position(currentLocation).title("Current Location"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        mMap.setOnMapClickListener { point ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLocation = LatLng(it.latitude, it.longitude)
                    val destination = point

                    getDirections(currentLocation, destination)
                }
            }
        }
    }

    private fun getDirections(origin: LatLng, destination: LatLng) {
        val apiKey = "AIzaSyAo0vlpu2GDLupGHCbn2jk9ESjadi-s20w"
        val geoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

        // Очистити всі полілінії на мапі
        mMap.clear()

        val startMarkerOptions = MarkerOptions().position(origin).title("Start")
        mMap.addMarker(startMarkerOptions)
        DirectionsApi.newRequest(geoApiContext)
            .mode(TravelMode.DRIVING)
            .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
            .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
            .setCallback(object : com.google.maps.PendingResult.Callback<DirectionsResult> {
                override fun onResult(result: DirectionsResult?) {
                    result?.let {
                        if (result.routes.isNotEmpty()) {
                            val route = result.routes[0]
                            val polylineOptions = PolylineOptions().color(Color.RED).width(5f)

                            route.legs.forEach { leg ->
                                leg.steps.forEach { step ->
                                    step.polyline.decodePath().forEach {
                                        polylineOptions.add(LatLng(it.lat, it.lng))
                                    }
                                }
                            }

                            runOnUiThread {
                                mMap.addPolyline(polylineOptions)
                            }
                            val endMarkerOptions = MarkerOptions().position(destination).title("Destination")
                            runOnUiThread {
                                mMap.addMarker(endMarkerOptions)
                            }
                        }
                    }
                }

                override fun onFailure(e: Throwable?) {
                    e?.let {
                        runOnUiThread {
                            Toast.makeText(
                                this@MapsActivity,
                                "Error: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}