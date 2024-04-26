package com.technifysoft.olxkotlin.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.RowImagesPickedBinding
import com.technifysoft.olxkotlin.models.ModelImagePicked
import java.lang.Exception

/**Constructor*
 * @param context The context of activity/fragment from where instance of AdapterImagesPicked class is created *
 * @param imagePickedArrayList The list of the images picked/captured from Gallery/Camera or from Internet
 * @param adId Id of the ad */
class AdapterImagePicked(
    private val context: Context,
    private val imagesPickedArrayList: ArrayList<ModelImagePicked>,
    private val adId: String
) : Adapter<AdapterImagePicked.HolderImagePicked>() {

    //View Binding
    private lateinit var binding: RowImagesPickedBinding

    private companion object {
        //Tag to show logs in logcat
        private const val TAG = "IMAGES_TAG"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImagePicked {
        //inflate/bind the row_images_picked.xml
        binding = RowImagesPickedBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderImagePicked(binding.root)
    }

    override fun onBindViewHolder(holder: HolderImagePicked, position: Int) {
        //get data from particular position of list and set to the UI Views of row_images_picked.xml and Handle clicks

        val model = imagesPickedArrayList[position]

        //check if image is from firebase storage or device storage
        if (model.fromInternet){
            //Image is from internet/firebase db. Get image Url of the image to set in imageIv
            try {
                //get imageUrl
                val imageUrl = model.imageUrl
                Log.d(TAG, "onBindViewHolder: imageUrl: $imageUrl")
                //set to image view i.e.imageIv
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_gray)
                    .into(holder.imageIv)
            } catch (e: Exception){
                Log.e(TAG, "onBindViewHolder: ", e)
            }

        } else {
            //Image is picked from Gallery/Camera. Get image Uri of the image to set in imageIv
            try {
                //get imageUri
                val imageUri = model.imageUri
                Log.d(TAG, "onBindViewHolder: imageUri: $imageUri")
                //set to image view i.e.imageIv
                Glide.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.ic_image_gray)
                    .into(holder.imageIv)
            } catch (e: Exception) {
                Log.e(TAG, "onBindViewHolder: ", e)
            }
        }



        // handle closeBtn click, if image is picked from device storage then just remove from list, if from firebase storage then first delete from firebase storage
        holder.closeBtn.setOnClickListener {
            //check if image is from Device Storage or Firebase
            if (model.fromInternet){

                deleteImageFirebase(model, holder, position)
            } else {

                imagesPickedArrayList.remove(model)
                notifyDataSetChanged()
            }


        }

    }

    private fun deleteImageFirebase(model: ModelImagePicked, holder: HolderImagePicked, position: Int) {
        //Id of the  image to delete  image
        val imageId = model.id

        Log.d(TAG, "deleteImageFirebase: adId: $adId")
        Log.d(TAG, "deleteImageFirebase: imageId: $imageId")

        //Ads > AdId > Images > ImageId
        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adId).child("Images").child(imageId)
            .removeValue()
            .addOnSuccessListener {
                //Delete Success
                Log.d(TAG, "deleteImageFirebase: Image $imageId deleted")
                Utils.toast(context, "Image deleted")
                //remove from imagesPickedArrayList
                try {
                    imagesPickedArrayList.remove(model)
                    notifyItemRemoved(position)
                } catch (e: Exception){
                    Log.d(TAG, "deleteImageFirebase1: ", e)
                }
            }
            .addOnFailureListener {e->
                //Delete Failure
                Log.e(TAG, "deleteImageFirebase2: ", e)
                Utils.toast(context, "Failed to delete image due to ${e.message}")
            }
    }

    override fun getItemCount(): Int {
        //return the size of list
        return imagesPickedArrayList.size
    }

    /** View holder class to hold/init UI Views of the row_images_picked.xml */
    inner class HolderImagePicked(itemView: View) : ViewHolder(itemView) {
        //UI Views of the row_images_picked.xml
        var imageIv = binding.imageIv
        var closeBtn = binding.closeBtn

    }
}