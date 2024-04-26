package com.technifysoft.olxkotlin.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.adapters.AdapterAd
import com.technifysoft.olxkotlin.databinding.ActivityAdSellerProfileBinding
import com.technifysoft.olxkotlin.models.ModelAd

class AdSellerProfileActivity : AppCompatActivity() {

    //View Binding
    private lateinit var binding: ActivityAdSellerProfileBinding

    private companion object {
        //TAG for logs in logcat
        private const val TAG  =  "SELLER_PROFILE_TAG"
    }

    //Seller UID, will get from intent
    private var sellerUid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init view binding... activity_ad_seller_profile.xml = ActivityAdSellerProfileBinding
        binding = ActivityAdSellerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get the uid of the seller of the  Ad (as we passed in AdDetailsActivity class while starting this activity)
        sellerUid = intent.getStringExtra("sellerUid").toString()
        Log.d(TAG, "onCreate: sellerUid:  $sellerUid")

        loadSellerDetails()
        loadAds()

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private fun loadSellerDetails(){
        Log.d(TAG, "loadSellerDetails: ")
        //Db path to load seller info. Users > sellerUid
        val ref =  FirebaseDatabase.getInstance().getReference("Users")
        ref.child(sellerUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get data. Spellings and letter case must be same as in firebase db
                    val name = "${snapshot.child("name").value}"
                    val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                    val timestamp = snapshot.child("timestamp").value as Long
                    //format date time e.g. timestamp to dd/MM/yyyy
                    val formattedDate = Utils.formatTimestampDate(timestamp)

                    //set data to UI Views
                    binding.sellerNameTv.text =  name
                    binding.sellerMemberSinceTv.text =  formattedDate
                    try {
                        Glide.with(this@AdSellerProfileActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(binding.sellerProfileIv)
                    } catch (e: Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadAds(){
        Log.d(TAG, "loadAds: ")

        //init adArrayList before starting adding data into it
        val adArrayList: ArrayList<ModelAd> = ArrayList()

        //Firebase DB listener to load ads of the seller using orderByChild query
        val ref  =  FirebaseDatabase.getInstance().getReference("Ads")
        ref.orderByChild("uid").equalTo(sellerUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear adArrayList each time starting adding data into it
                    adArrayList.clear()
                    //load ads list
                    for (ds in snapshot.children){
                        try {
                            //Prepare ModelAd with all data from Firebase DB
                            val modelAd = ds.getValue(ModelAd::class.java)
                            //Add the prepared ModelAd to list
                            adArrayList.add(modelAd!!)
                        } catch (e: java.lang.Exception){
                            Log.e(TAG, "onDataChange: ", e)
                        }
                    }

                    //Init/Setup AdapterAd and set to RecyclerView i.e. adsRv
                    val adapterAd = AdapterAd(this@AdSellerProfileActivity, adArrayList)
                    binding.adsRv.adapter = adapterAd

                    //set ads count
                    val adsCount = "${adArrayList.size}"
                    binding.publishedAdsCountTv.text = adsCount
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}