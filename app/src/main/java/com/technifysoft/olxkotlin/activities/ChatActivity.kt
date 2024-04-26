package com.technifysoft.olxkotlin.activities

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.adapters.AdapterChat
import com.technifysoft.olxkotlin.databinding.ActivityChatBinding
import com.technifysoft.olxkotlin.models.ModelChat
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {

    //View Binding
    private lateinit var binding:ActivityChatBinding


    private companion object {
        //TAG for logs in logcat
        private const val TAG = "CHAT_TAG"
    }

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    //Progress dialog to show while sending message
    private lateinit var progressDialog: ProgressDialog

    //UID of the receipt, will get from intent
    private var receiptUid = ""
    private var receiptFcmToken = ""

    //UID of the  current user
    private var myUid = ""
    private var myName = ""

    //Will generate using UIDs of current user and receipt
    private var chatPath = ""

    //Uri of the image picked from  Camera/Gallery
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init view binding... activity_chat.xml = ActivityChatBinding
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()

        //init/setup ProgressDialog to show while sending message
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //get the uid of the receipt (as we passed in ChatActivity class while starting this activity)
        receiptUid = intent.getStringExtra("receiptUid")!!
        //get uid of current signed-in user
        myUid = firebaseAuth.uid!!
        //chat path
        chatPath = Utils.chatPath(receiptUid, myUid)

        loadMyInfo()
        loadReceiptDetails()
        loadMessages()

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            finish()
        }

        //handle attachFab click, show image pick dialog
        binding.attachFab.setOnClickListener {
            imagePickDialog()
        }

        //handle sendBtn click, validate data before sending text message
        binding.sendFab.setOnClickListener {
            validateData()
        }

    }

    private fun loadMyInfo(){
        Log.d(TAG, "loadMyInfo: ")

        val  ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    myName = "${snapshot.child("name").value}"
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadReceiptDetails(){
        Log.d(TAG, "loadReceiptDetails: ")

        //Database reference to load receipt user info
        val  ref =   FirebaseDatabase.getInstance().getReference("Users")
        ref.child(receiptUid)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {

                    try {
                        //get user data, Note: Spellings must be same as in firebase db
                        val name = "${snapshot.child("name").value}"
                        val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                        receiptFcmToken = "${snapshot.child("fcmToken").value}"
                        Log.d(TAG, "onDataChange: name: $name")
                        Log.d(TAG, "onDataChange: profileImageUrl: $profileImageUrl")
                        Log.d(TAG, "onDataChange: receiptFcmToken: $receiptFcmToken")

                        //set user name
                        binding.toolbarTitleTv.text = name

                        //set user profile image
                        try {
                            Glide.with(this@ChatActivity)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_person_white)
                                .into(binding.toolbarProfileIv)
                        } catch (e: Exception){
                            Log.e(TAG, "onDataChange: ", )
                        }
                    } catch (e: Exception){
                        Log.e(TAG, "onDataChange: ", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun loadMessages(){
        Log.d(TAG, "loadMessages: ")
        //init chat arraylist
        val messageArrayList = ArrayList<ModelChat>()
        //Db reference to load chat messages
        val ref = FirebaseDatabase.getInstance().getReference("Chats")
        ref.child(chatPath)
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //clear adArrayList each time starting adding data into it
                    messageArrayList.clear()
                    //load messages list
                    for (ds: DataSnapshot in snapshot.children){

                        try {
                            //Prepare ModelChat with all data from Firebase DB
                            val modelChat = ds.getValue(ModelChat::class.java)
                            //add prepared model to adArrayList
                            messageArrayList.add(modelChat!!)
                        } catch (e: Exception){
                            Log.e(TAG, "onDataChange: ", e)
                        }
                    }

                    //init/setup AdapterChat class and set to recyclerview
                    val adapterChat = AdapterChat(this@ChatActivity, messageArrayList)
                    binding.chatRv.adapter = adapterChat

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun imagePickDialog(){
        Log.d(TAG, "imagePickDialog: ")

        //init popup menu param 1 is context and param 2 is the UI View (attachFab) to above/below we need to show popup menu
        val popupMenu = PopupMenu(this, binding.attachFab)
        //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, Param#3 is OrderID, Param#4 is Menu Item Title
        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        //Show Popup Menu
        popupMenu.show()
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener { menuItem ->
            //get the id of the menu item clicked
            val itemId = menuItem.itemId

            if (itemId == 1){
                //Camera is clicked we need to check if we have permission of Camera, Storage before launching Camera to Capture image
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    //Device version is TIRAMISU or above. We only need Camera permission
                    requestCameraPermissions.launch(arrayOf(Manifest.permission.CAMERA))
                } else {
                    //Device version is below TIRAMISU. We need Camera & Storage permissions
                    requestCameraPermissions.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            } else if (itemId == 2){
                //Gallery is clicked we need to check if we have permission of Storage before launching Gallery to Pick image
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    //Device version is TIRAMISU or above. We don't need Storage permission to launch Gallery
                    pickImageGallery()
                } else {
                    //Device version is below TIRAMISU. We need Storage permission to launch Gallery
                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            true
        }

    }

    private val requestCameraPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        //we will handle permission request result here to check if granted  or not
        Log.d(TAG, "requestCameraPermissions: ")


        var areAllGranted = true
        for (isGranted in result.values){
            areAllGranted = areAllGranted && isGranted
        }

        //let's check if permissions are granted or not
        if (areAllGranted){
            //All Permissions Camera, Storage are granted, we can now launch camera to capture image
            Log.d(TAG, "requestCameraPermissions: All permissions e.g. Camera and  Storage granted,")

            pickImageCamera()
        } else {
            //Camera or Storage or Both permissions are denied, Can't launch camera to capture image
            Log.d(TAG, "requestCameraPermissions: All permissions or some of Camera and Storage denied")
            Utils.toast(this, "All permissions or some of Camera and Storage denied")
        }
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        //we will handle permission request result here to check if granted  or not
        Log.d(TAG, "requestStoragePermission: isGranted: $isGranted")

        //let's check if permission is granted or not
        if (isGranted){
            //Storage Permission granted, we can now launch gallery to pick image
            pickImageGallery()
        } else {
            //Storage Permission denied, we can't launch gallery to pick image
            Utils.toast(this, "Permission denied...!")
        }

    }

    private fun pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ")

        //Setup Content values, MediaStore to capture high quality image using camera intent
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "THE_IMAGE_TITLE")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "THE_IMAGE_DESCRIPTION")
        //store the camera image in imageUri variable
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        //Intent to launch camera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        //Check if image is captured or not
        if (result.resultCode == Activity.RESULT_OK){
            //Image Captured, we have image in imageUri as assigned in pickImageCamera()
            Log.d(TAG, "cameraActivityResultLauncher: imageUri: $imageUri")

            //image picked, let's upload/send
            uploadToFirebaseStorage()
        } else {
            //Cancelled
            Utils.toast(this, "Cancelled...")
        }
    }

    private fun pickImageGallery(){
        Log.d(TAG, "pickImageGallery: ")

        //Intent to launch Image Picker e.g. Gallery
        val intent = Intent(Intent.ACTION_PICK)
        //We only want to pick images
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        //Check if image is picked or not
        if (result.resultCode == Activity.RESULT_OK){
            //get data
            val data = result.data
            //get uri of image picked
            imageUri = data!!.data
            Log.d(TAG, "galleryActivityResultLauncher: imageUri: $imageUri")

            //image picked, let's upload/send
            uploadToFirebaseStorage()
        } else {
            //Cancelled
            Utils.toast(this, "Cancelled...!")
        }
    }

    private fun uploadToFirebaseStorage(){
        Log.d(TAG, "uploadToFirebaseStorage: ")

        //show progress
        progressDialog.setMessage("Uploading image...!")
        progressDialog.show()

        //get timestamp for image name, and timestamp of message
        val timestamp = Utils.getTimestamp()
        //file path and name
        val filePathAndName = "ChatImages/$timestamp"
        //Storage reference to upload image
        val storageRef = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageRef.putFile(imageUri!!)
            .addOnProgressListener { snapshot ->
                //get current progress of image being uploaded
                val progress = 100.0*snapshot.bytesTransferred / snapshot.totalByteCount
                //set/update image upload progress to progress dialog
                progressDialog.setMessage("Uploading image: Progress ${progress.toUInt()} %")
            }
            .addOnSuccessListener {taskSnapshot ->
                //Image uploaded successfully, get url of uploaded image
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                //Url of image uploaded to firebase storage
                val uploadedImageUrl = uriTask.result.toString()

                if (uriTask.isSuccessful){
                    sendMessage(Utils.MESSAGE_TYPE_IMAGE, uploadedImageUrl, timestamp)
                }
            }
            .addOnFailureListener{ e->
                //Image upload failed
                progressDialog.dismiss()
                Log.e(TAG, "uploadToFirebaseStorage: ", e)
                Utils.toast(this, "Failed to upload due to ${e.message}")
            }

    }

    private fun validateData(){
        Log.d(TAG, "validateData: ")

        //input data
        val message = binding.messageEt.text.toString().trim()
        val timestamp = Utils.getTimestamp()

        //validate data
        if (message.isEmpty()){
            //No message entered, can't send
            Utils.toast(this, "Enter message to send...")
        } else {
            //Message entered, send
            sendMessage(Utils.MESSAGE_TYPE_TEXT, message, timestamp)
        }
    }

    private fun sendMessage(messageType: String, message: String, timestamp: Long){
        Log.d(TAG, "sendMessage: messageType: $messageType")
        Log.d(TAG, "sendMessage: message: $message")
        Log.d(TAG, "sendMessage: timestamp: $timestamp")

        //show progress
        progressDialog.setMessage("Sending message...!")
        progressDialog.show()

        //Database reference of Chats
        val refChat = FirebaseDatabase.getInstance().getReference("Chats")
        //key id to be used as message id
        val keyId = "${refChat.push().key}"

        //setup chat data in hashmap to add in firebase db
        val hashMap = HashMap<String, Any>()
        hashMap["messageId"] = "$keyId"             //String - message id
        hashMap["messageType"] = "$messageType"     //String - message type e.g. TEXT/IMAGE
        hashMap["message"] = "$message"             //String - text message or image url
        hashMap["fromUid"] = "$myUid"               //String - uid of sender i.e. currently logged-in user
        hashMap["toUid"] = "$receiptUid"            //String - uid of receiver/receipt i.e. the other user
        hashMap["timestamp"] = timestamp            //Long   - current timestamp

        //add chat data to firebase db e.g. Chats/uid1_uid2/messageId/____
        refChat.child(chatPath)
            .child(keyId)
            .setValue(hashMap)
            .addOnSuccessListener {
                //Message successfully sent/added
                Log.d(TAG, "sendMessage: message sent")
                progressDialog.dismiss()
                // after sending message clear message from messageEt
                binding.messageEt.setText("")


                //If message type is TEXT, pass the actual message to show as Notification description/body. If message type is IMAGE then pass "Sent an attachment"
                if (messageType == Utils.MESSAGE_TYPE_TEXT){
                    prepareNotification(message)
                } else {
                    prepareNotification("Sent an attachment")
                }
            }
            .addOnFailureListener {  e ->
                //Failed to send/add message
                Log.e(TAG, "sendMessage: ", e)

                progressDialog.dismiss()
                Utils.toast(this, "Failed to send due to ${e.message}")
            }
    }

    private fun prepareNotification(message: String){
        Log.d(TAG, "prepareNotification: ")

        //prepare json what to send, and where to send
        val notificationJo =  JSONObject()
        val notificationDataJo =  JSONObject()
        val notificationNotificationJo =  JSONObject()


        try {
            //extra/custom data
            notificationDataJo.put("notificationType", "${Utils.NOTIFICATION_TYPE_NEW_MESSAGE}")
            notificationDataJo.put("senderUid", "${firebaseAuth.uid}")
            //title, description, sound
            notificationNotificationJo.put("title", "$myName") //key "title" is reserved name in FCM API so be careful while typing
            notificationNotificationJo.put("body", "$message") //key "body" is reserved name in FCM API so be careful while typing
            notificationNotificationJo.put("sound", "default") //key "sound" is reserved name in FCM API so be careful while typing
            //combine all data in single JSON object
            notificationJo.put("to", "$receiptFcmToken") //"to" is reserved name in FCM API so be careful while typing
            notificationJo.put("notification", notificationNotificationJo) //key "notification" is reserved name in FCM API so be careful while typing
            notificationJo.put("data", notificationDataJo) //key "data" is reserved name in FCM API so be careful while typing
        } catch (e: Exception){
            Log.e(TAG, "prepareNotification: ", e)
        }

        sendFcmNotification(notificationJo)
    }

    private fun sendFcmNotification(notificationJo: JSONObject){
        //Prepare JSON Object Request to enqueue
        val jsonObjectRequest: JsonObjectRequest = object : JsonObjectRequest(
            Method.POST,
            "https://fcm.googleapis.com/fcm/send",
            notificationJo,
            Response.Listener {
                //Notification sent
                Log.d(TAG, "sendFcmNotification: Notification Send $it")
            },
            Response.ErrorListener {e->
                //Notification failed to send
                Log.e(TAG, "sendFcmNotification: ", e)
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                //put required headers
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "key=${Utils.FCM_SERVER_KEY}"

                return headers
            }
        }
        //enqueue the JSON Object Request
        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }
    
}