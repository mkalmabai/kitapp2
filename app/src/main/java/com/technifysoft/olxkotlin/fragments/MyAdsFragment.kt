package com.technifysoft.olxkotlin.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.technifysoft.olxkotlin.databinding.FragmentMyAdsBinding

class MyAdsFragment : Fragment() {
    //View Binding
    private lateinit var binding: FragmentMyAdsBinding

    private companion object {
        //TAG to show logs in logcat
        private const val TAG = "MY_ADS_TAG"
    }

    //Context for this fragment class
    private lateinit var mContext: Context


    private lateinit var myTabsViewPagerAdapter: MyTabsViewPagerAdapter

    override fun onAttach(context: Context) {
        //get and init the context for this fragment class
        this.mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate/bind the layout (fragment_my_ads.xml) for this fragment
        binding = FragmentMyAdsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Add the tabs to the TabLayout
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Ads"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Favorites"))

        //Fragment manage. initializing using getChildFragmentManager() because we are using tabs in fragment not activity (in activity we use getFragmentManager())
        val fragmentManager = childFragmentManager
        myTabsViewPagerAdapter = MyTabsViewPagerAdapter(fragmentManager, lifecycle)
        binding.viewPager.adapter = myTabsViewPagerAdapter

        //tab selected listener to set current item on view page
        binding.tabLayout.addOnTabSelectedListener(object: OnTabSelectedListener{

            override fun onTabSelected(tab: TabLayout.Tab) {
                //set current item on view page
                Log.d(TAG, "onTabSelected: tab: ${tab.position}")
                binding.viewPager.currentItem = tab.position
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }
        })

        //Change Tab when swiping
        binding.viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {

                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
            }
        })
    }


    class MyTabsViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun createFragment(position: Int): Fragment {
            //tab position starts from 0. if 0 set/show MyAdsAdsFragment otherwise it is definitely 1 so show MyAdsFavFragment
            if (position == 0){
                return MyAdsAdsFragment()
            } else {
                return MyAdsFavFragment()
            }
        }

        override fun getItemCount(): Int {
            //return list of items/tabs
            return 2 //setting static size 2 because we have two tabs/fragments
        }
    }
}