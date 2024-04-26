package com.technifysoft.olxkotlin.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.technifysoft.olxkotlin.RvListenerCategory
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.activities.LocationPickerActivity
import com.technifysoft.olxkotlin.adapters.AdapterAd
import com.technifysoft.olxkotlin.adapters.AdapterCategory
import com.technifysoft.olxkotlin.databinding.FragmentHomeBinding
import com.technifysoft.olxkotlin.models.ModelAd
import com.technifysoft.olxkotlin.models.ModelCategory

class HomeFragment : Fragment() {

    //View Binding
    private lateinit var binding: FragmentHomeBinding


    private companion object {
        //TAG to show logs in logcat
        private const val TAG = "HOME_TAG"
        //Max distance in kilometres to show ads under that distance
        private const val MAX_DISTANCE_TO_LOAD_ADS_KM = 10
    }

    //Context for this fragment class
    private lateinit var mContext: Context

    //adArrayList to hold ads list to show in RecyclerView
    private lateinit var adArrayList: ArrayList<ModelAd>

    //AdapterAd class instance to set to Recyclerview to show Ads list
    private lateinit var adapterAd: AdapterAd

    //SharedPreferences to store the selected location from map to load ads nearby
    private lateinit var locationSp: SharedPreferences

    //location info required to load ads nearby. We will get this info from the SharedPreferences saved after picking from map
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentAddress = ""

    override fun onAttach(context: Context) {
        //get and init the context for this fragment
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment i.e. fragment_home.xml
        binding = FragmentHomeBinding.inflate(LayoutInflater.from(mContext), container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //init the shared preferences param 1 is name of the Shared Preferences file, param 2 is mode of the SharedPreferences
        locationSp = mContext.getSharedPreferences("LOCATION_SP", Context.MODE_PRIVATE)

        //get saved current latitude, longitude, address from the Shared Preferences. In next steps we will pick these info from map and save in it
        currentLatitude = locationSp.getFloat("CURRENT_LATITUDE", 0.0f).toDouble()
        currentLongitude = locationSp.getFloat("CURRENT_LONGITUDE", 0.0f).toDouble()
        currentAddress = locationSp.getString("CURRENT_ADDRESS", "")!!

        //if current location is not 0 i.e. location is picked
        if (currentLatitude != 0.0 && currentLongitude != 0.0){
            //setting last selected location to locationTv
            binding.locationTv.text = currentAddress
        }

        //function call, load categories
        loadCategories()
        //function call, load all ads
        loadAds("All")

        //add text change listener to searchEt to search ads based on query typed in searchEt
        binding.searchEt.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d(TAG, "onTextChanged: Query: $s")
                try {
                    val query = s.toString()
                    adapterAd.filter.filter(query)
                } catch (e: Exception){
                    Log.e(TAG, "onTextChanged: ", e)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        //handle locationCv click, open LocationPickerActivity to pick location to ads nearby
        binding.locationCv.setOnClickListener {

            val intent = Intent(mContext, LocationPickerActivity::class.java)
            locationPickerActivityResultLauncher.launch(intent)
        }

    }


    private val locationPickerActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        //check if from map, location is picked or not
        if (result.resultCode == Activity.RESULT_OK){
            Log.d(TAG, "locationPickerActivityResultLauncher: RESULT_OK")

            val data = result.data

            if (data != null){
                Log.d(TAG, "locationPickerActivityResultLauncher: Location Picked!")
                //get location info from intent
                currentLatitude = data.getDoubleExtra("latitude", 0.0)
                currentLongitude = data.getDoubleExtra("longitude", 0.0)
                currentAddress = data.getStringExtra("address").toString()

                //save location info to shared preferences so when we launch app next time we don't need to pick again
                locationSp.edit()
                    .putFloat("CURRENT_LATITUDE", currentLatitude.toFloat())
                    .putFloat("CURRENT_LONGITUDE", currentLongitude.toFloat())
                    .putString("CURRENT_ADDRESS", currentAddress)
                    .apply()

                //set the picked address
                binding.locationTv.text = currentAddress

                //after picking address reload all ads again based on newly picked location
                loadAds("All")
            }
        } else {

            Utils.toast(mContext, "Cancelled!")
        }
    }

    private fun loadAds(category: String){
        Log.d(TAG, "loadAds: category: $category")
        //init adArrayList before starting adding data into it
        adArrayList = ArrayList()

        //Firebase DB listener to load ads based on Category & Distance
        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear adArrayList each time starting adding data into it
                adArrayList.clear()
                //load ads list
                for (ds in snapshot.children){

                    try {
                        //Prepare ModelAd with all data from Firebase DB
                        val modelAd = ds.getValue(ModelAd::class.java)
                        //function call with returned value as distance in kilometer.
                        val distance = calculateDistanceKm(
                            modelAd?.latitude ?: 0.0,
                            modelAd?.longitude ?: 0.0
                        )

                        Log.d(TAG, "onDataChange: distance: $distance")

                        //filter, add the Ad to list only if category is matched and is under specific distance i.e. 10km
                        if (category == "All"){
                            //Category All is selected, now check distance if is <= required e.g. 10km then show
                            if (distance <= MAX_DISTANCE_TO_LOAD_ADS_KM){
                                //the distance is <= required e.g. 10km. Add to list
                                adArrayList.add(modelAd!!)
                            }
                        } else {
                            //Some category is selected, so let's match if selected category matches with ad's category
                            if (modelAd!!.category.equals(category)){
                                //Selected category is matched with Ad's category, now check distance if is <= required e.g. 10km then show
                                if (distance <= MAX_DISTANCE_TO_LOAD_ADS_KM){
                                    //the distance is <= required e.g. 10km. Add to list
                                    adArrayList.add(modelAd)
                                }
                            }
                        }
                    } catch (e: Exception){

                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                //setup adapter and set to recyclerview
                adapterAd = AdapterAd(mContext, adArrayList)
                binding.adsRv.adapter = adapterAd
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun calculateDistanceKm(adLatitude: Double, adLongitude: Double) : Double{
        Log.d(TAG, "calculateDistanceKm: currentLatitude: $currentLatitude")
        Log.d(TAG, "calculateDistanceKm: currentLongitude: $currentLongitude")
        Log.d(TAG, "calculateDistanceKm: adLatitude: $adLatitude")
        Log.d(TAG, "calculateDistanceKm: adLongitude: $adLongitude")

        //Source Location i.e. user's current location
        val startPoint = Location(LocationManager.NETWORK_PROVIDER)
        startPoint.latitude = currentLatitude
        startPoint.longitude = currentLongitude

        //Destination Location i.e. Ad's location
        val endPoint = Location(LocationManager.NETWORK_PROVIDER)
        endPoint.latitude = adLatitude
        endPoint.longitude = adLongitude

        //calculate distance in meters
        val distanceInMeters = startPoint.distanceTo(endPoint).toDouble()
        //return distance in kilometers km = m/1000
        return distanceInMeters / 1000
    }

    private fun loadCategories(){
        //init categoryArrayList
        val categoryArrayList = ArrayList<ModelCategory>()

        //get categories from Utils class and add in categoryArrayList
        for (i in 0 until Utils.categories.size) {
            //ModelCategory instance to get/hold category from current index
            val modelCategory = ModelCategory(Utils.categories[i], Utils.categoryIcons[i])
            //add modelCategory to categoryArrayList
            categoryArrayList.add(modelCategory)
        }

        //init/setup AdapterCategory
        val adapterCategory = AdapterCategory(mContext, categoryArrayList, object:
            RvListenerCategory {
            override fun onCategoryClick(modelCategory: ModelCategory) {
                //get selected category
                val selectedCategory = modelCategory.category
                //load ads based on selected category
                loadAds(selectedCategory)
            }
        })

        //set adapter to the RecyclerView i.e. categoriesRv
        binding.categoriesRv.adapter = adapterCategory
    }

}