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
import com.technifysoft.olxkotlin.FilterChats
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.activities.ChatActivity
import com.technifysoft.olxkotlin.databinding.RowChatsBinding
import com.technifysoft.olxkotlin.models.ModelChats

class AdapterChats : RecyclerView.Adapter<AdapterChats.HolderChats>, Filterable{

    //Context of activity/fragment from where instance of AdapterChats class is created
    private var context: Context
    //chatsArrayList The list of the chats
    var chatsArrayList: ArrayList<ModelChats>
    private var filterList: ArrayList<ModelChats>

    private var filter: FilterChats? = null

    //View Binding
    private lateinit var binding: RowChatsBinding


    private companion object {
        //Tag to show logs in logcat
        private const val TAG = "ADAPTER_CHATS_TAG"
    }

    //Firebase Auth for auth related tasks
    private var firebaseAuth: FirebaseAuth

    //UID of currently logged-in user
    private var myUid = ""

    /**
     * Constructor*
     *
     * @param context     The context of activity/fragment from where instance of AdapterChats class is created *
     * @param chatsArrayList The list of chats
     */
    constructor(context: Context, chatsArrayList: ArrayList<ModelChats>) {
        this.context = context
        this.chatsArrayList = chatsArrayList
        this.filterList = chatsArrayList

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()

        //get the UID of currently logged-in user
        myUid = "${firebaseAuth.uid}"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChats {
        //inflate/bind the row_chats.xml
        binding = RowChatsBinding.inflate(LayoutInflater.from(context), parent, false)

        return HolderChats(binding.root)
    }

    override fun getItemCount(): Int {
        //return the size of list
        return chatsArrayList.size
    }

    override fun onBindViewHolder(holder: HolderChats, position: Int) {
        //get data from particular position of list and set to the UI Views of row_chats.xml and Handle clicks
        val modelChats = chatsArrayList[position]

        loadLastMessage(modelChats, holder)

        //handle chat item click, open ChatActivity
        holder.itemView.setOnClickListener {

            val receiptUid = modelChats.receiptUid

            if (receiptUid != null){
                val intent = Intent(context, ChatActivity::class.java)
                intent.putExtra("receiptUid", receiptUid)
                context.startActivity(intent)
            }
        }
    }

    private fun loadLastMessage(modelChats: ModelChats, holder: AdapterChats.HolderChats) {
        val chatKey = modelChats.chatKey
        Log.d(TAG, "loadLastMessage: chatKey: $chatKey")

        //Database reference to load last message info e.g. Chats > ChatKey > LastMessage
        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatKey).limitToLast(1)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    for (ds in snapshot.children){
                        //Get message data, spellings and data type must be same as in firebase db
                        val fromUid = "${ds.child("fromUid").value}"
                        val message = "${ds.child("message").value}"
                        val messageId = "${ds.child("messageId").value}"
                        val messageType = "${ds.child("messageType").value}"
                        val timestamp = ds.child("timestamp").value as Long ?: 0
                        val toUid = "${ds.child("toUid").value}"
                        //format message timestamp to proper date and time format e.g. 19/08/2023 09:30 AM
                        val formattedDate = Utils.formatTimestampDateTime(timestamp)

                        //set data to current instance of ModelChats using setters
                        modelChats.message = message
                        modelChats.messageId = messageId
                        modelChats.messageType = messageType
                        modelChats.fromUid = fromUid
                        modelChats.timestamp = timestamp
                        modelChats.toUid = toUid

                        //set formatted date and time
                        holder.dateTimeTv.text = "$formattedDate"

                        //check message type
                        if (messageType == Utils.MESSAGE_TYPE_TEXT){
                            //message type is TEXT, set last message
                            holder.lastMessageTv.text = message
                        } else {
                            //message type is IMAGE, just set hardcoded string e.g. Sends Attachment
                            holder.lastMessageTv.text = "Sends Attachment"
                        }
                    }

                    loadReceiptUserInfo(modelChats, holder)

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

    }

    private fun loadReceiptUserInfo(modelChats: ModelChats, holder: HolderChats) {

        val fromUid = modelChats.fromUid
        val toUid = modelChats.toUid
        //To identify either fromUid or toUid is the UID of the receipt we need to validate e.g. if fromUid == UID_OF_CURRENT_USER then receiptUid = toUid
        var receiptUid = ""
        if (fromUid == myUid){
            //fromUid = UID_OF_CURRENT_USER
            receiptUid = toUid
        } else {
            //fromUid != UID_OF_CURRENT_USER
            receiptUid = fromUid
        }

        Log.d(TAG, "loadReceiptUserInfo: fromUid: $fromUid")
        Log.d(TAG, "loadReceiptUserInfo: toUid: $toUid")
        Log.d(TAG, "loadReceiptUserInfo: receiptUid: $receiptUid")

        //set receiptUid to current instance of ModelChats using setters
        modelChats.receiptUid = receiptUid


        //Database reference to load receipt user info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(receiptUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //Get message data, spellings and data type must be same as in firebase db
                    val name = "${snapshot.child("name").value}"
                    val profileImageUrl = "${snapshot.child("profileImageUrl").value}"

                    //set data to current instance of ModelChats using setters
                    modelChats.name = name
                    modelChats.profileImageUrl = profileImageUrl

                    //set/show receipt name and profile image to UI
                    holder.nameTv.text = name
                    try {
                        Glide.with(context)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(holder.profileIv)
                    } catch (e: Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    override fun getFilter(): Filter {
        //init the filter obj only if it is null
        if (filter == null){
            filter = FilterChats(this, filterList)
        }

        return filter!!
    }


    inner class HolderChats(itemView: View) : RecyclerView.ViewHolder(itemView){
        //UI Views of the row_chats.xml
        var profileIv = binding.profileIv
        var nameTv = binding.nameTv
        var lastMessageTv = binding.lastMessageTv
        var dateTimeTv = binding.dateTimeTv
    }
}