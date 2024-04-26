package com.technifysoft.olxkotlin.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.ActivityLocationPickerBinding
import java.lang.Exception

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    //View Binding
    private lateinit var binding: ActivityLocationPickerBinding


    private companion object {
        //TAG for logs in logcat
        private const val TAG = "LOCATION_PICKER_TAG"
        //default zoom for map marker
        private const val DEFAULT_ZOOM = 15
    }


    private var mMap: GoogleMap? = null

    // Current Place Picker
    private var mPlaceClient: PlacesClient? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    // The geographical location where the device is currently located. That is, the last-known location retrieved by the Fused Location Provider.
    private var mLastKnownLocation: Location? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedAddress = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init view binding... activity_location_picker.xml = ActivityLocationPickerBinding
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //hide the doneLl for now. We will show when user select or search location
        binding.doneLl.visibility = View.GONE

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize the Places client
        Places.initialize(this, getString(R.string.my_google_map_api_key))

        // Create a new PlacesClient instance
        mPlaceClient = Places.createClient(this)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the AutocompleteSupportFragment to search place on map.
        val autocompleteSupportMapFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        // List of location fields we need in search result e.g. Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG
        val placesList = arrayOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        //set location fields list to the autocompleteSupportFragment
        autocompleteSupportMapFragment.setPlaceFields(listOf(*placesList))
        //listen for place selections
        autocompleteSupportMapFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener{
            override fun onPlaceSelected(place: Place) {
                Log.d(TAG, "onPlaceSelected: ")
                //Place selected. The param "place" contain all fields that we set as list.
                val id = place.id
                val name = place.name
                val latLng = place.latLng
                selectedLatitude = latLng?.latitude
                selectedLongitude = latLng?.longitude
                selectedAddress = place.address ?: ""

                Log.d(TAG, "onPlaceSelected: id: $id")
                Log.d(TAG, "onPlaceSelected: name: $name")
                Log.d(TAG, "onPlaceSelected: selectedLatitude: $selectedLatitude")
                Log.d(TAG, "onPlaceSelected: selectedLongitude: $selectedLongitude")
                Log.d(TAG, "onPlaceSelected: selectedAddress: $selectedAddress")

                addMarker(latLng, name, selectedAddress)
            }

            override fun onError(status: Status) {

            }
        })

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle toolbarGpsBtn click, if GPS enabled get and show user's current location
        binding.toolbarGpsBtn.setOnClickListener {
            //check if location enabled
            if (isGPSEnabled()){
                //GPS/Location enabled
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                //GPS/Location not enabled
                Utils.toast(this, "Location is not on! Turn it on to show current location")
            }
        }

        //handle doneBtn click, get the selected location back to requesting activity/fragment class
        binding.doneBtn.setOnClickListener {
            //put data to intent to get in previous activity
            val intent = Intent()
            intent.putExtra("latitude", selectedLatitude)
            intent.putExtra("longitude", selectedLongitude)
            intent.putExtra("address", selectedAddress)
            setResult(Activity.RESULT_OK, intent)
            //finishing this activity
            finish()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "onMapReady: ")
        mMap = googleMap

        // Prompt the user for permission.
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        //handle mMap click, get latitude, longitude when of where user clicked on map
        mMap!!.setOnMapClickListener { latLng ->
            //get latitude and longitude frm the param latLng
            selectedLatitude = latLng.latitude
            selectedLongitude = latLng.longitude
            Log.d(TAG, "onMapReady: selectedLatitude: $selectedLatitude")
            Log.d(TAG, "onMapReady: selectedLongitude: $selectedLongitude")

            //function call to get the address details from the latLng
            addressFromLatLng(latLng)
        }
    }

    @SuppressLint("MissingPermission")
    private val requestLocationPermission: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "requestLocationPermission: isGranted: $isGranted")
            //lets check if from permission dialog user have granted the permission or denied the result is in isGranted as true/false
            if (isGranted){
                //enable google map's gps button to set current location on map
                mMap!!.isMyLocationEnabled = true
                pickCurrentPlace()
            } else {
                //user denied permission so we can't pick location
                Utils.toast(this, "Permission denied")
            }
        }

    private fun addressFromLatLng(latLng: LatLng){
        Log.d(TAG, "addressFromLatLng: ")
        //init Geocoder class to get the address details from LatLng
        val geocoder = Geocoder(this)

        try {
            //get maximum 1 result (Address) from the list of available address list of addresses on basis of latitude and longitude we passed
            val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            //get Address object from the list addressList of type List<Address>
            val address = addressList!![0]
            //get the address details
            val addressLine = address.getAddressLine(0);
            val subLocality = address.subLocality
            //save address in selectedAddress variable
            selectedAddress = "$addressLine"
            //add marker on map
            addMarker(latLng, "$subLocality", "$addressLine")
        } catch (e: Exception){
            Log.e(TAG, "addressFromLatLng: ", e)
        }

    }

    /**
     * This function will be called only if location permission is granted.
     * We will only check if map object is not null then proceed to show location on map
     */
    private fun pickCurrentPlace(){
        Log.d(TAG, "pickCurrentPlace: ")
        if (mMap == null){
            return
        }

        detectAndShowDeviceLocationMap()
    }

    /**
     * Get the current location of the device, and position the map's camera
     */
    @SuppressLint("MissingPermission")
    private fun detectAndShowDeviceLocationMap(){
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            val locationResult = mFusedLocationProviderClient!!.lastLocation

            locationResult.addOnSuccessListener { location ->
                if (location != null){
                    //location got, save that location in mLastKnownLocation
                    mLastKnownLocation = location
                    //get latitude and longitude from location param
                    selectedLatitude = location.latitude
                    selectedLongitude = location.longitude
                    Log.d(TAG, "detectAndShowDeviceLocationMap: selectedLatitude: $selectedLatitude")
                    Log.d(TAG, "detectAndShowDeviceLocationMap: selectedLongitude: $selectedLongitude")

                    //setup LatLng from selectedLatitude and selectedLongitude
                    val latLng = LatLng(selectedLatitude!!, selectedLongitude!!)
                    mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat()))
                    mMap!!.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM.toFloat()))

                    //function call to retrieve the address from the latLng
                    addressFromLatLng(latLng)
                }
            }.addOnFailureListener { e->
                Log.e(TAG, "detectAndShowDeviceLocationMap: ", e)
            }

        } catch (e: Exception){
            Log.e(TAG, "detectAndShowDeviceLocationMap: ", e)
        }
    }

    /**
     * Check if GPS/Location is enabled or not
     */
    private fun isGPSEnabled() : Boolean {
        //init LocationManager
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //boolean variables to return values as true/false
        var gpsEnabled = false
        var networkEnabled = false
        //Check if GPS_PROVIDER enabled
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception){
            Log.e(TAG, "isGPSEnabled: ", e)
        }
        //Check if NETWORK_PROVIDER enabled
        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception){
            Log.e(TAG, "isGPSEnabled: ", e)
        }
        //return results
        return !(!gpsEnabled && !networkEnabled)
    }

    /**
     * Add Marker on map after searching/picking location
     *
     * @param latLng  LatLng of the location picked
     * @param title   Title of the location picked
     * @param address Address of the location picked
     */
    private fun addMarker(latLng: LatLng, title: String, address: String){
        Log.d(TAG, "addMarker: latitude: ${latLng.latitude}")
        Log.d(TAG, "addMarker: longitude: ${latLng.longitude}")
        Log.d(TAG, "addMarker: title: $title")
        Log.d(TAG, "addMarker: address: $address")
        //clear map before adding new marker. As we only need one Location Marker on map so if there is an already one clear it before adding new
        mMap!!.clear()

        try {
            //Setup marker options with latLng, address title, and complete address
            val markerOptions = MarkerOptions()
            markerOptions.position(latLng)
            markerOptions.title("$title")
            markerOptions.snippet("$address")
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

            //add marker to the map and move camera to the newly added marker
            mMap!!.addMarker(markerOptions)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat()))

            //show the doneLl, so user can go-back (with selected location) to the activity/fragment class that is requesting the location
            binding.doneLl.visibility = View.VISIBLE
            //set selected location complete address
            binding.selectedPlaceTv.text = address
        } catch (e: Exception){
            Log.e(TAG, "addMarker: ", e)
        }
    }
}