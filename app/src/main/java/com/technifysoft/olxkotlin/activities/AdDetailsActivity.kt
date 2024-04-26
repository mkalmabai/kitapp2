package com.technifysoft.olxkotlin.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.adapters.AdapterImageSlider
import com.technifysoft.olxkotlin.databinding.ActivityAdDetailsBinding
import com.technifysoft.olxkotlin.models.ModelAd
import com.technifysoft.olxkotlin.models.ModelImageSlider

class AdDetailsActivity : AppCompatActivity() {

    //View Binding
    private lateinit var binding: ActivityAdDetailsBinding

    private companion object{
        //TAG for logs in logcat
        private const val TAG = "AD_DETAILS_TAG"
    }

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    //Ad id, will get from intent
    private var adId = ""

    //Latitude & Longitude of the Ad to view it on Map
    private var adLatitude = 0.0
    private var adLongitude = 0.0

    //to load seller info, chat with seller, sms and call
    private var sellerUid = ""
    private var sellerPhone = ""

    //hold the Ad's favorite state by current user
    private var favorite = false

    //list of Ad's images to show in slider
    private lateinit var imageSliderArrayList: ArrayList<ModelImageSlider>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init view binding... activity_ad_details.xml = ActivityAdDetailsBinding
        binding = ActivityAdDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //hide some UI views in start. We will show the Edit, Delete option if the user is Ad owner. We will show Call, Chat, SMS option if user isn't Ad owner
        binding.toolbarEditBtn.visibility = View.GONE
        binding.toolbarDeleteBtn.visibility = View.GONE
        binding.chatBtn.visibility = View.GONE
        binding.callBtn.visibility = View.GONE
        binding.smsBtn.visibility = View.GONE

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()

        //get the id of the Ad (as we passed in AdapterAd class while starting this activity)
        adId = intent.getStringExtra("adId").toString()
        Log.d(TAG, "onCreate: adId: $adId")

        //if user is logged-in then check if the Ad is in favorites of the user
        if (firebaseAuth.currentUser!= null){
            checkIsFavorite()
        }

        loadAdDetails()
        loadAdImages()

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle toolbarDeleteBtn click, delete Ad
        binding.toolbarDeleteBtn.setOnClickListener {
            //Alert dialog to confirm if the user really wants to delete the Ad
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(this)
            materialAlertDialogBuilder.setTitle("Delete Ad")
                .setMessage("Are you sure you want to delete this Ad?")
                .setPositiveButton("DELETE"){ dialog, which ->
                    //Delete Clicked, delete Ad
                    Log.d(TAG, "onCreate: DELETE clicked...")
                    deleteAd()
                }
                .setNegativeButton("CANCEL"){dialog, which ->
                    //Cancel Clicked, dismiss dialog
                    Log.d(TAG, "onCreate: CANCEL clicked...")
                    dialog.dismiss()
                }
                .show()
        }

        //handle toolbarEditBtn click, show options (Edit, Mark as sode) dialog
        binding.toolbarEditBtn.setOnClickListener {
            editOptionsDialog()
        }

        //handle toolbarFavBtn click, add/remove favorite
        binding.toolbarFavBtn.setOnClickListener {
            //if favorite remove from favorite, if not favorite add to favorite
            if (favorite){
                //this Ad is in favorite of current user, remove from favorite
                Utils.removeFromFavorite(this, adId)
            } else {
                //this Ad is not in favorite of current user, add to favorite
                Utils.addToFavorite(this, adId)
            }
        }

        //handle sellerProfileCv click, start SellerProfileActivity
        binding.sellerProfileCv.setOnClickListener {
            val  intent  = Intent(this, AdSellerProfileActivity::class.java)
            intent.putExtra("sellerUid", sellerUid)
            startActivity(intent)
        }

        //handle chatBtn click, start ChatActivity
        binding.chatBtn.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiptUid", sellerUid)
            startActivity(intent)
        }

        //handle callBtn click, open Ad Creator's phone number in dialer
        binding.callBtn.setOnClickListener {
            Utils.callIntent(this, sellerPhone)
        }

        //handle smsBtn click, open Ad Creator's phone number in sms
        binding.smsBtn.setOnClickListener {
            Utils.smsIntent(this, sellerPhone)
        }

        //handle mapBtn click, open map with Ad location
        binding.mapBtn.setOnClickListener {
            Utils.mapIntent(this, adLatitude, adLongitude)
        }

    }

    private fun editOptionsDialog(){
        Log.d(TAG, "editOptionsDialog: ")
        //init/setup popup menu
        val popupMenu = PopupMenu(this, binding.toolbarEditBtn)

        //Add menu items to PopupMenu with params Group ID, Item ID, Order, Title
        popupMenu.menu.add(Menu.NONE, 0, 0, "Edit")
        popupMenu.menu.add(Menu.NONE, 1, 1, "Mark As Sold")

        //show popup menu
        popupMenu.show()

        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener { menuItem ->
            //get id of the menu item clicked
            val itemId = menuItem.itemId
            //based ob menuItem id, check which menu item is clicked
            if (itemId == 0){
                //Edit Clicked, start the AdCreateActivity with Ad Id and isEditMode as true
                val intent = Intent(this, AdCreateActivity::class.java)
                intent.putExtra("isEditMode", true)
                intent.putExtra("adId", adId)
                startActivity(intent)
            } else if (itemId == 1){
                //Mark As Sold
                showMarkAsSoldDialog()
            }

            return@setOnMenuItemClickListener true
        }
    }

    private fun showMarkAsSoldDialog(){
        Log.d(TAG, "showMarkAsSoldDialog: ")
        //Material Alert Dialog - Setup and show
        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
        alertDialogBuilder.setTitle("Mark as sold")
            .setMessage("Are you sure you want to mark this Ad as sold?")
            .setPositiveButton("SOLD"){ dialog, which ->
                Log.d(TAG, "showMarkAsSoldDialog: SOLD clicked")

                //setup info to update in the existing Ad i.e. mark as sold by setting the value of status to SOLD
                val hashMap = HashMap<String, Any>()
                hashMap["status"] = "${Utils.AD_STATUS_SOLD}"

                //Ad's db path to update its available/sold status. Ads > AdId
                val ref = FirebaseDatabase.getInstance().getReference("Ads")
                ref.child(adId)
                    .updateChildren(hashMap)
                    .addOnSuccessListener {
                        //Success
                        Log.d(TAG, "showMarkAsSoldDialog: Marked as sold")
                    }
                    .addOnFailureListener {e ->
                        //Failure
                        Log.e(TAG, "showMarkAsSoldDialog: ", e)
                        Utils.toast(this, "Failed to mark as sold due to ${e.message}")
                    }
            }
            .setNegativeButton("CANCEL"){ dialog, which ->

                Log.d(TAG, "showMarkAsSoldDialog: CANCEL clicked")
                dialog.dismiss()
            }
            .show()
    }

    private fun loadAdDetails(){
        Log.d(TAG, "loadAdDetails: ")
        //Ad's db path to get the Ad details. Ads > AdId
        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adId)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    try {
                        //setup model from firebase DataSnapshot
                        val modelAd = snapshot.getValue(ModelAd::class.java)

                        //get data from model
                        sellerUid = "${modelAd!!.uid}"
                        val title = modelAd.title
                        val description = modelAd.description
                        val address = modelAd.address
                        val condition = modelAd.condition
                        val category = modelAd.category
                        val price = modelAd.price
                        adLatitude = modelAd.latitude
                        adLongitude = modelAd.longitude
                        val timestamp = modelAd.timestamp

                        //format date time e.g. timestamp to dd/MM/yyyy
                        val formattedDate = Utils.formatTimestampDate(timestamp)

                        //check if the Ad is by currently signed-in user
                        if (sellerUid == firebaseAuth.uid){
                            //Ad is created by currently signed-in user so
                            //1) Should be able to edit and delete Ad
                            binding.toolbarEditBtn.visibility = View.VISIBLE
                            binding.toolbarDeleteBtn.visibility = View.VISIBLE
                            //2) Shouldn't able to chat, call, sms (to himself), view seller profile
                            binding.chatBtn.visibility = View.GONE
                            binding.callBtn.visibility = View.GONE
                            binding.smsBtn.visibility = View.GONE
                            binding.sellerProfileLabelTv.visibility = View.GONE
                            binding.sellerProfileCv.visibility = View.GONE
                        } else {
                            //Ad is not created by currently signed in user so
                            //1) Shouldn't be able to edit and delete Ad
                            binding.toolbarEditBtn.visibility = View.GONE
                            binding.toolbarDeleteBtn.visibility = View.GONE
                            //2) Should be able to chat, call, sms (to Ad creator), view seller profile
                            binding.chatBtn.visibility = View.VISIBLE
                            binding.callBtn.visibility = View.VISIBLE
                            binding.smsBtn.visibility = View.VISIBLE
                            binding.sellerProfileLabelTv.visibility = View.VISIBLE
                            binding.sellerProfileCv.visibility = View.VISIBLE
                        }

                        //set data to UI Views
                        binding.titleTv.text = title
                        binding.descriptionTv.text = description
                        binding.addressTv.text = address
                        binding.conditionTv.text = condition
                        binding.categoryTv.text = category
                        binding.priceTv.text = price
                        binding.dateTv.text = formattedDate

                        //function call, load seller info e.g. profile image, name, member since
                        loadSellerDetails()

                    } catch (e: Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadSellerDetails(){
        Log.d(TAG, "loadSellerDetails: ")
        //Db path to load seller info. Users > sellerUid
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(sellerUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get data, keys (spellings, case) must be same as in firebase db
                    val phoneCode = "${snapshot.child("phoneCode").value}"
                    val phoneNumber = "${snapshot.child("phoneNumber").value}"
                    val name = "${snapshot.child("name").value}"
                    val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                    val timestamp = snapshot.child("timestamp").value as Long

                    //format date time e.g. timestamp to dd/MM/yyyy
                    val formattedDate = Utils.formatTimestampDate(timestamp)
                    //phone number of seller
                    sellerPhone = "$phoneCode$phoneNumber"

                    //set data to UI Views
                    binding.sellerNameTv.text = name
                    binding.memberSinceTv.text = formattedDate
                    try {
                        Glide.with(this@AdDetailsActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(binding.sellerProfileIv)
                    } catch (e: java.lang.Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun checkIsFavorite(){
        Log.d(TAG, "checkIsFavorite: ")
        //DB path to check if Ad is in Favorite of current user. Users > uid > Favorites > adId
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child("${firebaseAuth.uid}").child("Favorites").child(adId)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //if snapshot exists (value is true) means the Ad is in favorite of current user otherwise no
                    favorite = snapshot.exists()
                    Log.d(TAG, "onDataChange: favorite: $favorite")

                    //check if favorite or not to set image of favBtn accordingly
                    if (favorite){
                        //Favorite, set image ic_fav_yes to button favBtn
                        binding.toolbarFavBtn.setImageResource(R.drawable.ic_fav_yes)
                    } else{
                        //Not Favorite, set image ic_fav_no to button favBtn
                        binding.toolbarFavBtn.setImageResource(R.drawable.ic_fav_no)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadAdImages(){
        Log.d(TAG, "loadAdImages: ")

        //init list before starting adding data into it
        imageSliderArrayList = ArrayList()

        //Db path to load the Ad images. Ads > AdId > Images
        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adId).child("Images")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear list before starting adding data into it
                    imageSliderArrayList.clear()
                    //there might be multiple images, loop it to load all
                    for (ds in snapshot.children){

                        try {
                            //prepare model (spellings in model class should be same as in firebase)
                            val modelImageSlider = ds.getValue(ModelImageSlider::class.java)
                            //add the prepared model to list
                            imageSliderArrayList.add(modelImageSlider!!)
                        } catch (e: Exception){
                            Log.e(TAG, "onDataChange: ", e)
                        }

                    }
                    //setup adapter and set to viewpager i.e. imageSliderVp
                    val adapterImageSlider = AdapterImageSlider(this@AdDetailsActivity, imageSliderArrayList)
                    binding.imageSliderVp.adapter = adapterImageSlider
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }


    private fun deleteAd(){
        Log.d(TAG, "deleteAd: ")
        //Db path to delete the Ad. Ads > AdId
        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adId)
            .removeValue()
            .addOnSuccessListener {
                //Success
                Log.d(TAG, "deleteAd: Deleted")
                Utils.toast(this, "Deleted...!")
                //finish activity and go-back
                finish()
            }
            .addOnFailureListener {e->
                //Failure
                Log.e(TAG, "deleteAd: ", e)
                Utils.toast(this, "Failed to delete due to ${e.message}")
            }
    }


}