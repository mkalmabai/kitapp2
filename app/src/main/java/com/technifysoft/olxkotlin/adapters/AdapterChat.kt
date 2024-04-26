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
import com.google.firebase.auth.FirebaseAuth
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.models.ModelChat

class AdapterChat : Adapter<AdapterChat.HolderChat>{


    private val context: Context
    private val chatArrayList: ArrayList<ModelChat>

    companion object {
        //TAG for logs in logcat
        private const val TAG = "ADAPTER_CHAT_TAG"

        //a constant to indicate, the left/receipt ui i.e. row_chat_left.xml
        private const val MSG_TYPE_LEFT = 0

        //a constant to indicate, the right/current-user ui i.e. row_chat_right.xml
        private const val MSG_TYPE_RIGHT = 1
    }

    //To get currently signed-in user
    private val firebaseAuth: FirebaseAuth

    /** constructor
     * @param context Context of activity/fragment from where instance of AdapterAd class is created
     * @param chatArrayList The list of the messages*/
    constructor(context: Context, chatArrayList: ArrayList<ModelChat>) {
        this.context = context
        this.chatArrayList = chatArrayList

        //get instance of firebase auth for Auth related tasks e.g. get currently signed-in user
        firebaseAuth = FirebaseAuth.getInstance()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderChat {
        //inflate the layouts row_chat_left.xml and row_chat_right.xml
        if (viewType == MSG_TYPE_RIGHT){
            //based on condition implemented in getItemViewType() the UI type is row_chat_right.xml (message by currently logged-in user)
            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent,false)

            return HolderChat(view)
        } else {
            //based on condition implemented in getItemViewType() the UI type is row_chat_left.xml (message by receipt user)
            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false)

            return HolderChat(view)
        }
    }


    override fun onBindViewHolder(holder: HolderChat, position: Int) {
        //get data from particular position of list and set to the UI Views of row_chat_left.xml and row_chat_right.xml and Handle clicks
        val modelChat = chatArrayList[position]

        //get data
        val message = modelChat.message
        val messageType = modelChat.messageType
        val timestamp = modelChat.timestamp
        //format date time e.g. dd/MM/yyyy hh:mm:a (03/08/2023 08:30 AM)
        val formattedDate = Utils.formatTimestampDateTime(timestamp)
        //set formatted date and time to timeTv
        holder.timeTv.text = formattedDate

        if (messageType == Utils.MESSAGE_TYPE_TEXT){
            //Message type is TEXT. Show messageTv and hide imageIv
            holder.messageTv.visibility = View.VISIBLE
            holder.messageIv.visibility = View.GONE

            //set text message to TextView i.e. messageTv
            holder.messageTv.text = message
        } else {
            //Message type is IMAGE. Hide messageTv and show imageIv
            holder.messageTv.visibility = View.GONE
            holder.messageIv.visibility = View.VISIBLE

            //set image to ImageView i.e. imageIv
            try {
                Glide.with(context)
                    .load(message)
                    .placeholder(R.drawable.ic_image_gray)
                    .error(R.drawable.ic_image_broker_gray)
                    .into(holder.messageIv)
            } catch (e: Exception){
                Log.e(TAG, "onBindViewHolder: ", e)
            }
        }
    }

    override fun getItemCount(): Int {
        //return the size of list | number of items in list
        return chatArrayList.size
    }

    override fun getItemViewType(position: Int): Int {
        //if the fromUid == current_user_uid then message is by currently logged-in user otherwise message is from receipt
        if (chatArrayList[position].fromUid == firebaseAuth.uid){
            //fromUid == current_user_uid, message is by currently logged-in user, will show row_chat_right.xml
            return MSG_TYPE_RIGHT
        } else {
            //fromUid != current_user_uid, message is by receipt user, will show row_chat_left.xml
            return MSG_TYPE_LEFT
        }
    }

    inner class HolderChat(itemView: View) : RecyclerView.ViewHolder(itemView){
        //init UI Views of the row_chat_left.xml & row_chat_right.xml
        var messageTv: TextView = itemView.findViewById(R.id.messageTv)
        var timeTv: TextView = itemView.findViewById(R.id.timeTv)
        var messageIv: ShapeableImageView = itemView.findViewById(R.id.messageIv)
    }
}