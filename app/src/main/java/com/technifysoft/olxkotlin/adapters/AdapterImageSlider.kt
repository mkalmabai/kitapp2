package com.technifysoft.olxkotlin.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.databinding.RowImageSliderBinding
import com.technifysoft.olxkotlin.models.ModelImageSlider
import java.lang.Exception

class AdapterImageSlider : Adapter<AdapterImageSlider.HolderImageSlider>{
    //View Binding
    private lateinit var binding: RowImageSliderBinding

    private companion object {
        //Tag to show logs in logcat
        private const val TAG = "IMAGE_SLIDER_TAG"
    }

    //Context of activity/fragment from where instance of AdapterAd class is created
    private var context: Context
    //imageSliderArrayList The list of the images
    private var imageArrayList: ArrayList<ModelImageSlider>


    /**
     * Constructor
     *
     * @param context The context of activity/fragment from where instance of AdapterAd class is created *
     * @param imageArrayList The list of images
     */
    constructor(context: Context, imageArrayList: ArrayList<ModelImageSlider>) {
        this.context = context
        this.imageArrayList = imageArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderImageSlider {
        //inflate/bind the row_image_slider.xml
        binding = RowImageSliderBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderImageSlider(binding.root)
    }

    override fun onBindViewHolder(holder: HolderImageSlider, position: Int) {
        //get data from particular position of list and set to the UI Views of row_ad.xml and Handle clicks
        val modelImageSlider = imageArrayList[position]

        //get url of the image
        val imageUrl = modelImageSlider.imageUrl
        //Show current image/total images e.g. 1/3 here 1 is current image and 3 is total images
        val imageCount = "${position+1}/${imageArrayList.size}"
        //set image count
        holder.imageCountTv.text = imageCount
        //set image
        try {
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_gray)
                .into(holder.imageIv)
        } catch (e: Exception){
            Log.e(TAG, "onBindViewHolder: ", e)
        }

        //handle image click, open in full screen e.g. ImageViewActivity
        holder.itemView.setOnClickListener {

        }
    }


    override fun getItemCount(): Int {
        //return the size of list
        return imageArrayList.size
    }

    inner class HolderImageSlider(itemView: View): RecyclerView.ViewHolder(itemView){

        //init UI Views of the row_ad.xml
        var imageIv: ShapeableImageView = binding.imageIv
        var imageCountTv: TextView = binding.imageCountTv
    }

}