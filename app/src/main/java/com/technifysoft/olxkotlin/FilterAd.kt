package com.technifysoft.olxkotlin

import android.widget.Filter
import com.technifysoft.olxkotlin.adapters.AdapterAd
import com.technifysoft.olxkotlin.models.ModelAd
import java.util.Locale

/**
 * Filter Ad Constructor
 *
 * @param adapter    AdapterAd instance to be passed when this constructor is created
 * @param filterList ad arraylist to be passed when this constructor is created
 */
class FilterAd(
    private val adapter: AdapterAd,
    private val filterList: ArrayList<ModelAd>
) : Filter(){

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        //in this function we will perform filter based on what user type
        var constraint = constraint
        val results = FilterResults()

        //check if query is not empty and not null
        if (!constraint.isNullOrEmpty()){
            //query neither empty nor null
            //convert to uppercase to make query not case sensitive, you can also to lowercase
            constraint = constraint.toString().uppercase(Locale.getDefault())
            //to hold list of filtered ads based on query
            val filteredModels = ArrayList<ModelAd>()
            for (i in filterList.indices){
                //apply filter if query matches to any of brand, category condition, title then add it to the filteredModels
                if (filterList[i].brand.uppercase(Locale.getDefault()).contains(constraint) ||
                    filterList[i].category.uppercase(Locale.getDefault()).contains(constraint) ||
                    filterList[i].condition.uppercase(Locale.getDefault()).contains(constraint) ||
                    filterList[i].title.uppercase(Locale.getDefault()).contains(constraint)
                ) {
                    //query matches to any of brand, category condition, title then add it to the filteredModels
                    filteredModels.add(filterList[i])
                }
            }
            //prepare filtered list and item count
            results.count = filteredModels.size
            results.values = filteredModels
        } else {
            //query is either empty or null, prepare original/complete list and item count
            results.count = filterList.size
            results.values = filterList
        }

        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {

        adapter.adArrayList = results.values as ArrayList<ModelAd>

        adapter.notifyDataSetChanged()
    }

}