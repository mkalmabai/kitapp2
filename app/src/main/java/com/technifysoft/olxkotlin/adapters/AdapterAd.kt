package com.technifysoft.olxkotlin.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.technifysoft.olxkotlin.FilterAd
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.activities.AdDetailsActivity
import com.technifysoft.olxkotlin.databinding.RowAdBinding
import com.technifysoft.olxkotlin.models.ModelAd
import java.lang.Exception

class AdapterAd : RecyclerView.Adapter<AdapterAd.HolderAd>, Filterable{

    //View Binding
    private lateinit var binding: RowAdBinding

    private companion object {
        //Tag to show logs in logcat
        private const val TAG = "ADAPTER_AD_TAG"
    }
    //Context of activity/fragment from where instance of AdapterAd class is created
    private var context: Context
    //adArrayList The list of the Ads
    var adArrayList: ArrayList<ModelAd>
    private var filterList: ArrayList<ModelAd>

    private var filter: FilterAd? = null

    //Firebase Auth for auth related tasks
    private var firebaseAuth: FirebaseAuth


    /**
     * Constructor*
     *
     * @param context     The context of activity/fragment from where instance of AdapterAd class is created *
     * @param adArrayList The list of ads
     */
    constructor(context: Context, adArrayList: ArrayList<ModelAd>) {
        this.context = context
        this.adArrayList = adArrayList
        this.filterList = adArrayList

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderAd {
        //inflate/bind the row_ad.xml
        binding = RowAdBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderAd(binding.root)
    }

    override fun onBindViewHolder(holder: HolderAd, position: Int) {
        //get data from particular position of list and set to the UI Views of row_ad.xml and Handle clicks
        val modelAd = adArrayList[position]

        val title = modelAd.title
        val description = modelAd.description
        val address = modelAd.address
        val condition = modelAd.condition
        val price = modelAd.price
        val timestamp = modelAd.timestamp
        val formattedDate = Utils.formatTimestampDate(timestamp)

        //function call: load first image from available images of Ad e.g. if there are 5 images of Ad, load first one
        loadAdFirstImage(modelAd, holder)

        //if user is logged in then check that if the Ad is in favorite of current user
        if (firebaseAuth.currentUser != null){
            checkIsFavorite(modelAd, holder)
        }

        //set data to UI Views of row_ad.xml
        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.addressTv.text = address
        holder.conditionTv.text = condition
        holder.priceTv.text = price
        holder.dateTv.text = formattedDate

        //handle itemView (i.e. Ad) click, open the AdDetailsActivity. also pass the id of the Ad to intent to load details
        holder.itemView.setOnClickListener {

            val intent = Intent(context, AdDetailsActivity::class.java)
            intent.putExtra("adId", modelAd.id)
            context.startActivity(intent)
        }

        //handle favBtn click, add/remove the ad to/from favorite of current user
        holder.favBtn.setOnClickListener {
            //check if ad is in favorite of current user or not - true/false
            val favorite = modelAd.favorite
            if (favorite){
                //this Ad is in favorite of current user, remove from favorite
                Utils.removeFromFavorite(context, modelAd.id)
            } else {
                //this Ad is not in favorite of current user, add to favorite
                Utils.addToFavorite(context, modelAd.id)
            }
        }
    }

    private fun checkIsFavorite(modelAd: ModelAd, holder: HolderAd) {
        //DB path to check if Ad is in Favorite of current user. Users > uid > Favorites > adId
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!).child("Favorites").child(modelAd.id)
            .addValueEventListener(object : ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {
                    //if snapshot exists (value is true) means the Ad is in favorite of current user otherwise no
                    val favorite = snapshot.exists()
                    //set that value (true/false) to model
                    modelAd.favorite = favorite
                    //check if favorite or not to set image of favBtn accordingly
                    if (favorite) {
                        //Favorite, set image ic_fav_yes to button favBtn
                        holder.favBtn.setImageResource(R.drawable.ic_fav_yes)
                    } else {
                        //Not Favorite, set image ic_fav_no to button favBtn
                        holder.favBtn.setImageResource(R.drawable.ic_fav_no)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadAdFirstImage(modelAd: ModelAd, holder: HolderAd) {
        //load first image from available images of Ad e.g. if there are 5 images of Ad, load first one
        //Ad id to get image of it
        val adId = modelAd.id

        Log.d(TAG, "loadAdFirstImage: adId: $adId")

        val reference = FirebaseDatabase.getInstance().getReference("Ads")
        reference.child(adId).child("Images").limitToFirst(1)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //this will return only 1 image as we have used query .limitToFirst(1)
                    for (ds in snapshot.children){
                        //get url of the image, make sure spellings are same as in firebase db
                        val imageUrl = "${ds.child("imageUrl").value}"
                        Log.d(TAG, "onDataChange: imageUrl: $imageUrl")


                        //set image to Image Vew i.e. imageIv
                        try {
                            Glide.with(context)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_image_gray)
                                .into(holder.imageIv)
                        } catch (e: Exception){
                            Log.e(TAG, "onDataChange: ", e)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    override fun getItemCount(): Int {
        //return the size of list
        return adArrayList.size
    }

    override fun getFilter(): Filter {
        //init the filter obj only if it is null
        if (filter == null){
            filter = FilterAd(this, filterList)
        }

        return filter as FilterAd
    }

    /** HolderAd a ViewHolder class to hold/init UI Views of row_ad.xml*/
    inner class HolderAd(itemView: View) : RecyclerView.ViewHolder(itemView){

        //init UI Views of the row_ad.xml
        var imageIv = binding.imageIv
        var titleTv = binding.titleTv
        var descriptionTv = binding.descriptionTv
        var favBtn = binding.favBtn
        var addressTv = binding.addressTv
        var conditionTv = binding.conditionTv
        var priceTv = binding.priceTv
        var dateTv = binding.dateTv
    }


}