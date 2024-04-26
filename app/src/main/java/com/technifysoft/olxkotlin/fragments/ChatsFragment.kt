package com.technifysoft.olxkotlin.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.adapters.AdapterChats
import com.technifysoft.olxkotlin.databinding.FragmentChatsBinding
import com.technifysoft.olxkotlin.models.ModelChats

class ChatsFragment : Fragment() {
    //View Binding
    private lateinit var binding: FragmentChatsBinding


    private companion object{
        //TAG to show logs in logcat
        private const val TAG = "CHATS_TAG"
    }

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    //UID of currently logged-in user
    private var myUid = ""

    //Context for this fragment class
    private lateinit var mContext: Context

    //chatsArrayList to hold chats list by currently logged-in user to show in RecyclerView
    private lateinit var chatsArrayList: ArrayList<ModelChats>

    //AdapterChats class instance to set to Recyclerview to show chats list
    private lateinit var adapterChats: AdapterChats

    override fun onAttach(context: Context) {
        //get and init the context for this fragment class
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate/bind the layout (fragment_chats.xml) for this fragment
        binding = FragmentChatsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()

        myUid = "${firebaseAuth.uid}"
        Log.d(TAG, "onViewCreated: myUid: $myUid")

        loadChats()

        //add text change listener to searchEt to search chats using filter applied in AdapterChats class
        binding.searchEt.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                try {
                    val query = s.toString()
                    Log.d(TAG, "onTextChanged: Search Query: $query")

                    adapterChats.filter.filter(query)
                } catch (e: Exception){
                    Log.e(TAG, "onTextChanged: ", e)
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    private fun loadChats(){
        //init chatsArrayList before starting adding data into it
        chatsArrayList = ArrayList()

        //Firebase DB listener to get the chats of logged-in user.
        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                //clear chatsArrayList each time starting adding data into it
                chatsArrayList.clear()
                //load chats, we only need chatKey e.g. uid1_uid2 here, we have to get (already done) the chat data, and receipt user data in adapter class
                for (ds in snapshot.children){
                    //The chat key e.g. uid1_uid2
                    val chatKey = "${ds.key}"
                    Log.d(TAG, "onDataChange: chatKey: $chatKey")
                    //if chat key uid1_uid2 contains the uid of currently logged-in user will be considered as chat of currently logged-in user
                    if (chatKey.contains(myUid)){
                        Log.d(TAG, "onDataChange: Contains, Add to list")
                        //Create instance of ModelChats and add the chatKey in it
                        val modelChats = ModelChats()
                        modelChats.chatKey = chatKey
                        //add the instance of ModelChats in chatsArrayList
                        chatsArrayList.add(modelChats)
                    } else {
                        Log.d(TAG, "onDataChange: Not contains, Skip")
                    }
                }

                //init/setup adapter class and set to recyclerview
                adapterChats = AdapterChats(mContext, chatsArrayList)
                binding.chatsRv.adapter = adapterChats

                //after loading data in list we  will sort the list using timestamp of each last message of chat, to show the newest chat first
                sort()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun sort() {
        //Delay of 1 second before sorting the list
        Handler().postDelayed({
            //sort chatsArrayList
            chatsArrayList.sortWith { model1: ModelChats, model2: ModelChats ->
                model2.timestamp.compareTo(model1.timestamp)
            }

            //notify changes
            adapterChats.notifyDataSetChanged()

        }, 1000)
    }

}