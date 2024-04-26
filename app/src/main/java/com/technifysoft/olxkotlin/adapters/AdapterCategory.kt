package com.technifysoft.olxkotlin.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.technifysoft.olxkotlin.RvListenerCategory
import com.technifysoft.olxkotlin.databinding.RowCategoryBinding
import com.technifysoft.olxkotlin.models.ModelCategory
import java.util.Random

/**AdapterCategory
* @param context The context of activity/fragment from where instance of AdapterCategory class is created
 * @param categoryArrayList The list of categories
 * @param rvListenerCategory instance of the RvListenerCategory interface*/
class AdapterCategory(
    private val context: Context,
    private val categoryArrayList: ArrayList<ModelCategory>,
    private val rvListenerCategory: RvListenerCategory
) : Adapter<AdapterCategory.HolderCategory>(){


    private lateinit var binding: RowCategoryBinding

    private companion object {
        private const val TAG = "ADAPTER_CATEGORY_TAG"
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderCategory {
        //inflate/bind the row_category.xml
        binding = RowCategoryBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderCategory(binding.root)
    }

    override fun onBindViewHolder(holder: HolderCategory, position: Int) {
        //get data from particular position of list and set to the UI Views of row_category.xml and Handle clicks
        val modelCategory = categoryArrayList[position]

        //get data from modelCategory
        val icon = modelCategory.icon
        val category = modelCategory.category

        //get random color to set as background color of the categoryIconIv
        val random = Random()
        val color = Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255))

        //set data to UI Views of row_category.xml
        holder.categoryIconIv.setImageResource(icon)
        holder.categoryTv.text = category
        holder.categoryIconIv.setBackgroundColor(color)

        //handle item click, call interface (RvListenerCategory) method to perform click in calling activity/fragment class instead of this class
        holder.itemView.setOnClickListener {

            rvListenerCategory.onCategoryClick(modelCategory)
        }
    }


    override fun getItemCount(): Int {
        //return the size of list
        return categoryArrayList.size
    }

    inner class HolderCategory (itemView: View) : ViewHolder(itemView){
        //init UI Views of the row_category.xml
        var categoryIconIv = binding.categoryIconIv
        var categoryTv = binding.categoryTv

    }



}