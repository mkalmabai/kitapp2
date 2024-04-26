package com.technifysoft.olxkotlin

import android.widget.Filter
import com.technifysoft.olxkotlin.adapters.AdapterChats
import com.technifysoft.olxkotlin.models.ModelChats
import java.util.Locale

class FilterChats : Filter {

    //declaring AdapterChats and ArrayList<ModelChats> instance that will be initialized in constructor of this class
    private val adapterChats: AdapterChats
    private val filterList: ArrayList<ModelChats>

    /**
     * Filter Chats Constructor
     *
     * @param adapter    AdapterChats instance to be passed when this constructor is created
     * @param filterList chats arraylist to be passed when this constructor is created
     */
    constructor(adapterChats: AdapterChats, filterList: ArrayList<ModelChats>) : super() {
        this.adapterChats = adapterChats
        this.filterList = filterList
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        //perform filter based on what user type
        var constraint: CharSequence? = constraint
        val results = FilterResults()

        if (!constraint.isNullOrEmpty()){
            //the search query is not null and not empty, we can perform filter, convert the typed query to upper case
            //to make search not case sensitive e.g. Atif Pervaiz -> ATIF PERVAIZ
            constraint = constraint.toString().uppercase()

            //hold the filtered list of Ads based on user searched query
            val filteredModels = ArrayList<ModelChats>()
            for (i in filterList.indices){
                //filter based on Receipt User Name. If matches add it to the filteredModels list
                if (filterList[i].name.uppercase().contains(constraint)){
                    //Filter matched add to filteredModels list
                    filteredModels.add(filterList[i])
                }
            }

            //the search query has matched item(s), we can perform filter. Return filteredModels list
            results.count = filteredModels.size
            results.values = filteredModels

        } else {
            //the search query is either null or empty, we can't perform filter. Return full/original list
            results.count = filterList.size
            results.values = filterList
        }


        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        //publish the filtered result
        adapterChats.chatsArrayList  = results.values as ArrayList<ModelChats>
        adapterChats.notifyDataSetChanged()
    }

}