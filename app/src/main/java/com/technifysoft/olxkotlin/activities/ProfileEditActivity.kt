package com.technifysoft.olxkotlin.activities

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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.ActivityProfileEditBinding

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding


    private companion object{
        private const val TAG = "PROFILE_EDIT_TAG"
    }

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    //ProgressDialog to show while profile update
    private lateinit var progressDialog: ProgressDialog


    private var myUserType = ""


    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //activity_profile_edit.xml = ActivityProfileEditBinding
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //ProgressDialog to show while profile update
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()
        loadMyInfo()

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle profileImagePickFab click, show image pick popup menu
        binding.profileImagePickFab.setOnClickListener {
            imagePickDialog()
        }

        //handle updateBtn click, validate data
        binding.updateBtn.setOnClickListener { 
            validateData()
        }
        
    }
    
    
    private var name = ""
    private var dob = ""
    private var email = ""
    private var phoneCode = ""
    private var phoneNumber = ""
    
    private fun validateData(){
        //input data
        name = binding.nameEt.text.toString().trim()
        dob = binding.dobEt.text.toString().trim()
        email = binding.emailEt.text.toString().trim()
        phoneCode = binding.countryCodePicker.selectedCountryCodeWithPlus
        phoneNumber = binding.phoneNumberEt.text.toString().trim()

        //validate data
        if (imageUri == null){
            //no image to upload to storage, just update db
            updateProfileDb(null)
        } else {
            //image need to upload to storage, first upload image then update db
            uploadProfileImageStorage()
        }
    }
    
    private fun uploadProfileImageStorage(){
        Log.d(TAG, "uploadProfileImageStorage: ")
        //show progress
        progressDialog.setMessage("Uploading user profile image")
        progressDialog.show()

        //setup image name and path e.g. UserImages/profile_useruid
        val filePathAndName = "UserProfile/profile_${firebaseAuth.uid}"
        //Storage reference to upload image
        val ref = FirebaseStorage.getInstance().reference.child(filePathAndName)
        ref.putFile(imageUri!!)
            .addOnProgressListener { snapshot ->
                //check image upload progress and show
                val progress = 100.0* snapshot.bytesTransferred / snapshot.totalByteCount
                Log.d(TAG, "uploadProfileImageStorage: progress: $progress")
                progressDialog.setMessage("Uploading profile image. Progress: $progress")
            }
            .addOnSuccessListener { taskSnapshot ->
                //Image uploaded successfully, get url of uploaded image
                Log.d(TAG, "uploadProfileImageStorage: Image uploaded...")

                val uriTask = taskSnapshot.storage.downloadUrl

                while(!uriTask.isSuccessful);

                val uploadedImageUrl = uriTask.result.toString()
                if (uriTask.isSuccessful){
                    updateProfileDb(uploadedImageUrl)
                }

            }
            .addOnFailureListener { e->
                //Failed to upload image
                Log.e(TAG, "uploadProfileImageStorage: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to upload due to ${e.message}")
            }
    }

    private fun updateProfileDb(uploadedImageUrl: String?){
        Log.d(TAG, "updateProfileDb: uploadedImageUrl: $uploadedImageUrl")
        //show progress
        progressDialog.setMessage("Updating user info")
        progressDialog.show()
        //setup data in hashmap to update to firebase db
        val hashMap = HashMap<String, Any>()
        hashMap["name"] = "$name"
        hashMap["dob"] = "$dob"
        if (uploadedImageUrl != null){
            //update profileImageUrl in db only if uploaded image url is not null
            hashMap["profileImageUrl"] = "$uploadedImageUrl"
        }
        //if user type is Phone then allow to update email otherwise (in case of Google or Email) allow to update Phone
        if (myUserType.equals("Phone", true)){
            //User type is Phone allow to update Email not Phone
            hashMap["email"] = "$email"
        } else if(myUserType.equals("Email", true) || myUserType.equals("Google", true)){
            //User type is Email or Google allow to update Phone not Email
            hashMap["phoneCode"] = "$phoneCode"
            hashMap["phoneNumber"] = "$phoneNumber"
        }

        //Database reference of user to update info
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child("${firebaseAuth.uid}")
            .updateChildren(hashMap)
            .addOnSuccessListener {
                //profile updated successfully
                Log.d(TAG, "updateProfileDb: Updated...")
                progressDialog.dismiss()
                Utils.toast(this, "Updated...")

                imageUri = null
            }
            .addOnFailureListener { e ->
                //failed to update profile, show exception in log and exception message in toast
                Log.e(TAG, "updateProfileDb: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to update due to ${e.message}")
            }
    }

    private fun loadMyInfo(){
        Log.d(TAG, "loadMyInfo: ")
        //Reference of current user info in Firebase Realtime Database to get user info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child("${firebaseAuth.uid}")
            .addValueEventListener(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info, spellings should be same as in firebase realtime database
                    val dob = "${snapshot.child("dob").value}"
                    val email = "${snapshot.child("email").value}"
                    val name = "${snapshot.child("name").value}"
                    val phoneCode = "${snapshot.child("phoneCode").value}"
                    val phoneNumber = "${snapshot.child("phoneNumber").value}"
                    val profileImageUrl = "${snapshot.child("profileImageUrl").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    myUserType = "${snapshot.child("userType").value}"

                    //concatenate phone code and phone number to make full phone number
                    val phone = phoneCode + phoneNumber
                    //Check User Type, if Email/Google then don't allow user to edit/update email
                    if (myUserType.equals("Email", true) || myUserType.equals("Google", true)){
                        //user type is Email or Google. Don't allow to edit email
                        binding.emailTil.isEnabled = false
                        binding.emailEt.isEnabled = false
                    }
                    else{
                        //user type is Phone. Don't allow to edit phone
                        binding.phoneNumberTil.isEnabled = false
                        binding.phoneNumberEt.isEnabled = false
                        binding.countryCodePicker.isEnabled = false
                    }
                    //set data to UI
                    binding.emailEt.setText(email)
                    binding.dobEt.setText(dob)
                    binding.nameEt.setText(name)
                    binding.phoneNumberEt.setText(phoneNumber)

                    try {
                        val phoneCodeInt = phoneCode.replace("+", "").toInt()  //e.g. +92 ---> 92
                        binding.countryCodePicker.setCountryForPhoneCode(phoneCodeInt)
                    } catch (e: Exception) {
                        Log.e(TAG, "onDataChange: ", e)
                    }

                    //Set profile image to profileIv
                    try {
                        Glide.with(this@ProfileEditActivity)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(binding.profileIv)
                    } catch (e: Exception) {
                        Log.e(TAG, "onDataChange: ", e)
                    }


                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }


    private fun imagePickDialog(){
        //init popup menu param 1 is context and param 2 is the UI View (profileImagePickFab) to above/below we need to show popup menu
        val popupMenu = PopupMenu(this, binding.profileImagePickFab)
        //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, Param#3 is OrderID, Param#4 is Menu Item Title
        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        //Show Popup Menu
        popupMenu.show()
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener { item ->
            //get id of the menu item clicked
            val itemId = item.itemId

            if (itemId == 1){
                //Camera is clicked we need to check if we have permission of Camera, Storage before launching Camera to Capture image
                Log.d(TAG, "imagePickDialog: Camera Clicked, check if camera permission(s) granted or not")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    //Device version is TIRAMISU or above. We only need Camera permission
                    requestCameraPermissions.launch(arrayOf(android.Manifest.permission.CAMERA))
                }
                else{
                    //Device version is below TIRAMISU. We need Camera & Storage permissions
                    requestCameraPermissions.launch(arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
                }
            }
            else if (itemId == 2){
                //Gallery is clicked we need to check if we have permission of Storage before launching Gallery to Pick image
                Log.d(TAG, "imagePickDialog: Gallery Clicked, check if storage permission granted or not")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    //Device version is TIRAMISU or above. We don't need Storage permission to lanuch Gallery
                    pickImageGallery()
                }
                else{
                    //Device version is below TIRAMISU. We need Storage permission to lanuch Gallery
                    requestStoragePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }

            return@setOnMenuItemClickListener true
        }
    }


    private val requestCameraPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ result ->
            Log.d(TAG, "requestCameraPermissions: result: $result")

            //let's check if permissions are granted or not
            var areAllGranted = true
            for (isGranted in result.values){
                areAllGranted = areAllGranted && isGranted
            }

            if (areAllGranted){
                //All Permissions Camera, Storage are granted, we can now launch camera to capture image
                Log.d(TAG, "requestCameraPermissions: All granted e.g. Camera, Storage")
                pickImageCamera()
            }else{//Camera or Storage or Both permissions are denied, Can't launch camera to capture image
                Log.d(TAG, "requestCameraPermissions: All or either one is denied...")
                Utils.toast(this, "Camera or Storage or both permissions denied")
            }

        }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()){ isGranted ->
            Log.d(TAG, "requestStoragePermission: isGranted $isGranted")
            //let's check if permission is granted or not
            if (isGranted){
                //Storage Permission granted, we can now launch gallery to pick image
                pickImageGallery()
            } else{
                //Storage Permission denied, we can't launch gallery to pick image
                Utils.toast(this, "Storage permission denied...")
            }
        }


    private fun pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ")
        //Setup Content values, MediaStore to capture high quality image using campera intent
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_image_title")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp_image_description")
        //store captured image in variable imageUri
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        //Intent to launch camera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)

    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->

            //Check if image is captured or not
            if (result.resultCode == Activity.RESULT_OK) {
                //Image Captured, we have image in imageUri as assigned in pickImageCamera()
                Log.d(TAG, "cameraActivityResultLauncher: Image captured: imageUri: $imageUri")
                //set to profileIv
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.profileIv)
                } catch (e: Exception) {
                    Log.e(TAG, "cameraActivityResultLauncher: ", e)
                }
            } else {
                //Cancelled
                Utils.toast(this, "Cancelled!")
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

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
            //Check if image is picked or not
            if (result.resultCode == Activity.RESULT_OK){
                //get data
                val data = result.data
                //get uri of image picked
                imageUri = data!!.data

                //set to profileIv
                try {
                    Glide.with(this)
                        .load(imageUri)
                        .placeholder(R.drawable.ic_person_white)
                        .into(binding.profileIv)
                }
                catch (e: java.lang.Exception){
                    Log.e(TAG, "galleryActivityResultLauncher: ", e)
                }
            }
        }
}