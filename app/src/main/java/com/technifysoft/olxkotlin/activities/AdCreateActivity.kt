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
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.adapters.AdapterImagePicked
import com.technifysoft.olxkotlin.databinding.ActivityAdCreateBinding
import com.technifysoft.olxkotlin.models.ModelImagePicked

class AdCreateActivity : AppCompatActivity() {

    //View Binding
    private lateinit var binding: ActivityAdCreateBinding

    private companion object {
        //TAG for logs in logcat
        private const val TAG = "ADD_CREATE_TAG"
    }

    //ProgressDialog to show while adding/updating the Ad
    private lateinit var progressDialog: ProgressDialog

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    //Image Uri to hold uri of the image (picked/captured using Gallery/Camera) to add in Ad Images List
    private var imageUri: Uri? = null

    //list of images (picked/captured using Gallery/Camera or from internet)
    private lateinit var imagePickedArrayList: ArrayList<ModelImagePicked>

    //adapter to be set in RecyclerView that will load list of images (picked/captured using Gallery/Camera or from internet)
    private lateinit var adapterImagePicked: AdapterImagePicked

    private var isEditMode = false
    private var adIdForEditing = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //init view binding... activity_ad_create.xml = ActivityAdCreateBinding
        binding = ActivityAdCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init/setup ProgressDialog to show while adding/updating the Ad
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()

        //Setup and set the categories adapter to the Category Input Filed i.e. categoryAct
        val adapterCategories = ArrayAdapter(this, R.layout.row_category_act, Utils.categories)
        binding.categoryAct.setAdapter(adapterCategories)

        //Setup and set the conditions adapter to the Condition Input Filed i.e. conditionAct
        val adapterConditions = ArrayAdapter(this, R.layout.row_condtion_act, Utils.conditions)
        binding.conditionAct.setAdapter(adapterConditions)

        //get value of isEditMode from intent to check if we are here to Create New Ad (came from MainActivity), to Update existing Ad (came from AdDetailsActivity)
        isEditMode = intent.getBooleanExtra("isEditMode", false)
        Log.d(TAG, "onCreate: isEditMode: $isEditMode")

        //check if we are here to Create New Ad (came from MainActivity), to Update existing Ad (came from AdDetailsActivity)
        if (isEditMode){
            //Edit Ad Mode: Get the Ad Id for editing the Ad
            adIdForEditing = intent.getStringExtra("adId") ?: ""

            //function call to load Ad details by using Ad Id
            loadAdDetails()

            //change toolbar title and submit button text
            binding.toolbarTitleTv.text = "Update Ad"
            binding.postAdBtn.text = "Update Ad"
        } else {
            //New Ad Mode: Change toolbar title and submit button text
            binding.toolbarTitleTv.text = "Create Ad"
            binding.postAdBtn.text = "Post Ad"
        }

        //init imagePickedArrayList
        imagePickedArrayList = ArrayList()
        //loadImages
        loadImages()

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle toolbarAddImageBtn click, show image add options (Gallery/Camera)
        binding.toolbarAdImageBtn.setOnClickListener {
            showImagePickOptions()
        }

        //handle locationAct click, launch LocationPickerActivity to pick location from MAP
        binding.locationAct.setOnClickListener {
            val intent = Intent(this, LocationPickerActivity::class.java)
            locationPickerActivityResultLauncher.launch(intent)
        }

        //handle postAdBtn click, validate data and publish the Ad
        binding.postAdBtn.setOnClickListener {
            validateData()
        }
    }

    private val locationPickerActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            Log.d(TAG, "locationPickerActivityResultLauncher: ")
            //get result of location picked from LocationPickerActivity
            if (result.resultCode == Activity.RESULT_OK){
                //get data:Intent from result param
                val data = result.data
                //check if data is not null then handle/assign the data
                if (data != null){
                    latitude = data.getDoubleExtra("latitude", 0.0)
                    longitude = data.getDoubleExtra("longitude", 0.0)
                    address = data.getStringExtra("address") ?: ""

                    Log.d(TAG, "locationPickerActivityResultLauncher: latitude: $latitude")
                    Log.d(TAG, "locationPickerActivityResultLauncher: longitude: $longitude")
                    Log.d(TAG, "locationPickerActivityResultLauncher: address: $address")
                    //set address to locationAct
                    binding.locationAct.setText(address)
                }

            } else {
                Log.d(TAG, "locationPickerActivityResultLauncher: cancelled")
                Utils.toast(this, "Cancelled")
            }
        }

    private fun loadImages(){
        Log.d(TAG, "loadImages: ")
        //init setup adapterImagesPicked to set it RecyclerView i.e. imagesRv. Param 1 is Context, Param 2 is Images List to show in RecyclerView
        adapterImagePicked = AdapterImagePicked(this, imagePickedArrayList, adIdForEditing)
        //set the adapter to the RecyclerView i.e. imagesRv
        binding.imagesRv.adapter = adapterImagePicked
    }


    private fun showImagePickOptions(){
        Log.d(TAG, "showImagePickOptions: ")
        //init the PopupMenu. Param 1 is context. Param 2 is Anchor view for this popup. The popup will appear below the anchor if there is room, or above it if there is not.
        val popupMenu = PopupMenu(this, binding.toolbarAdImageBtn)
        //add menu items to our popup menu Param#1 is GroupID, Param#2 is ItemID, Param#3 is OrderID, Param#4 is Menu Item Title
        popupMenu.menu.add(Menu.NONE, 1, 1, "Camera")
        popupMenu.menu.add(Menu.NONE, 2, 2, "Gallery")
        //Show Popup Menu
        popupMenu.show()
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener { item ->
            //get the id of the item clicked in popup menu
            val itemId = item.itemId
            //check which item id is clicked from popup menu. 1=Camera. 2=Gallery as we defined
            if (itemId == 1){
                //Camera is clicked we need to check if we have permission of Camera, Storage before launching Camera to Capture image
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    //Device version is TIRAMISU or above. We only need Camera permission
                    val cameraPermissions = arrayOf(Manifest.permission.CAMERA)
                    requestCameraPermission.launch(cameraPermissions)
                } else {
                    //Device version is below TIRAMISU. We need Camera & Storage permissions
                    val cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestCameraPermission.launch(cameraPermissions)
                }

            } else if (itemId == 2){
                //Gallery is clicked we need to check if we have permission of Storage before launching Gallery to Pick image
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    //Device version is TIRAMISU or above. We don't need Storage permission to launch Gallery
                    pickImageGallery()
                } else {
                    //Device version is below TIRAMISU. We need Storage permission to launch Gallery
                    val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
                    requestStoragePermission.launch(storagePermission)
                }
            }

            true
        }
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {isGranted ->
        Log.d(TAG, "requestStoragePermission: isGranted: $isGranted")
        //let's check if permission is granted or not
        if (isGranted){
            //Storage Permission granted, we can now launch gallery to pick image
            pickImageGallery()
        } else {
            //Storage Permission denied, we can't launch gallery to pick image
            Utils.toast(this, "Storage permission denied...")
        }
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        Log.d(TAG, "requestCameraPermission: result: $result")
        //let's check if permissions are granted or not
        var areAllGranted = true
        for (isGranted in result.values){
            areAllGranted = areAllGranted && isGranted
        }

        if (areAllGranted){
            //All Permissions Camera, Storage are granted, we can now launch camera to capture image
            pickImageCamera()
        } else {
            //Camera or Storage or Both permissions are denied, Can't launch camera to capture image
            Utils.toast(this, "Camera or Storage or both permissions denied....")
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

    private fun pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ")
        //Setup Content values, MediaStore to capture high quality image using camera intent
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "TEMP_IMAGE_TITLE")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "TEMP_IMAGE_DESCRIPTION")
        //Uri of the image to be captured from camera
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        //Intent to launch camera
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {result ->
        Log.d(TAG, "galleryActivityResultLauncher: ")
        //Check if image is picked or not
        if (result.resultCode == Activity.RESULT_OK){
            //get data from result param
            val data = result.data
            //get uri of image picked
            imageUri = data!!.data
            Log.d(TAG, "galleryActivityResultLauncher: imageUri: $imageUri")

            //timestamp will be used as id of the image picked
            val timestamp = "${Utils.getTimestamp()}"

            //setup model for image. Param 1 is id, Param 2 is imageUri, Param 3 is imageUrl, fromInternet
            val modelImagePicked = ModelImagePicked(timestamp, imageUri, null, false)
            //add model to the imagePickedArrayList
            imagePickedArrayList.add(modelImagePicked)

            //reload the images
            loadImages()
        } else {
            //Cancelled
            Utils.toast(this, "Cancelled...!")
        }
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {result ->
        Log.d(TAG, "cameraActivityResultLauncher: ")
        //Check if image is picked or not
        if (result.resultCode == Activity.RESULT_OK){
            //no need to get image uri here we will have it in pickImageCamera() function
            Log.d(TAG, "cameraActivityResultLauncher: imageUri $imageUri")

            //timestamp will be used as id of the image picked
            val timestamp = "${Utils.getTimestamp()}"

            //setup model for image. Param 1 is id, Param 2 is imageUri, Param 3 is imageUrl, fromInternet
            val modelImagePicked = ModelImagePicked(timestamp, imageUri, null, false)
            //add model to the imagePickedArrayList
            imagePickedArrayList.add(modelImagePicked)

            //reload the images
            loadImages()
        } else {
            //Cancelled
            Utils.toast(this, "Canncelled...!")
        }
    }

    //variables to hold Ad data
    private var brand = ""
    private var category = ""
    private var condition = ""
    private var address = ""
    private var price = ""
    private var title = ""
    private var description = ""
    private var latitude = 0.0
    private var longitude = 0.0


    private fun validateData(){
        Log.d(TAG, "validateData: ")
        //input data
        brand = binding.brandEt.text.toString().trim()
        category = binding.categoryAct.text.toString().trim()
        condition = binding.conditionAct.text.toString().trim()
        address = binding.locationAct.text.toString().trim()
        price = binding.priceEt.text.toString().trim()
        title = binding.titleEt.text.toString().trim()
        description = binding.descriptionEt.text.toString().trim()

        //validate data
        if (brand.isEmpty()){
            //no brand entered in brandEt, show error in brandEt and focus
            binding.brandEt.error = "Enter Brand"
            binding.brandEt.requestFocus()
        } else if (category.isEmpty()) {
            //no categoryAct entered in categoryAct, show error in categoryAct and focus
            binding.categoryAct.error = "Choose Category"
            binding.categoryAct.requestFocus()
        } else if (condition.isEmpty()){
            //no conditionAct entered in conditionAct, show error in conditionAct and focus
            binding.conditionAct.error = "Choose Condition"
            binding.conditionAct.requestFocus()
        } else if (title.isEmpty()){
            //no titleEt entered in titleEt, show error in titleEt and focus
            binding.titleEt.error = "Enter Title"
            binding.titleEt.requestFocus()
        } else if (description.isEmpty()) {
            //no descriptionEt entered in descriptionEt, show error in descriptionEt and focus
            binding.descriptionEt.error = "Enter Description"
            binding.descriptionEt.requestFocus()
        } else {
            //All data is validated, we can proceed further now

            if (isEditMode){

                updateAd()
            } else {

                postAd()
            }
        }

    }

    private fun postAd(){
        Log.d(TAG, "postAd: ")

        progressDialog.setMessage("Publishing Ad")
        progressDialog.show()

        //get current timestamp
        val timestamp = Utils.getTimestamp()
        //firebase database Ads reference to store new Ads
        val refAds = FirebaseDatabase.getInstance().getReference("Ads")
        //key id from the reference to use as Ad id
        val keyId = refAds.push().key

        //setup data to add in firebase database
        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$keyId"
        hashMap["uid"] = "${firebaseAuth.uid}"
        hashMap["brand"] = "$brand"
        hashMap["category"] = "$category"
        hashMap["condition"] = "$condition"
        hashMap["address"] = "$address"
        hashMap["price"] = "$price"
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["status"] = "${Utils.AD_STATUS_AVAILABLE}"
        hashMap["timestamp"] = timestamp
        hashMap["latitude"] = latitude
        hashMap["longitude"] = longitude


        //set data to firebase database. Ads -> AdId -> AdDataJSON
        refAds.child(keyId!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "postAd: Ad Published")
                uploadImagesStorage(keyId)
            }
            .addOnFailureListener {e ->

                Log.e(TAG, "postAd: ", e)
                progressDialog.dismiss()
                Utils.toast(this, " Failed due to ${e.message}")
            }
    }


    private fun updateAd(){
        Log.d(TAG, "updateAd: ")

        progressDialog.setMessage("Updating Ad...")
        progressDialog.show()

        //setup data to add in firebase database
        val hashMap = HashMap<String, Any>()
        hashMap["brand"] = "$brand"
        hashMap["category"] = "$category"
        hashMap["condition"] = "$condition"
        hashMap["address"] = "$address"
        hashMap["price"] = "$price"
        hashMap["title"] = "$title"
        hashMap["description"] = "$description"
        hashMap["latitude"] = latitude
        hashMap["longitude"] = longitude

        //Db path to update Ad. Ads > AdId > DataToUpdate
        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adIdForEditing)
            .updateChildren(hashMap)
            .addOnSuccessListener {
                //Ad data update success
                Log.d(TAG, "updateAd: Ad Updated...")
                progressDialog.dismiss()
                //start uploading images picked for the Ad
                uploadImagesStorage(adIdForEditing)
            }
            .addOnFailureListener {e->
                //Ad data update failed
                Log.e(TAG, "updateAd: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to update the Ad due to ${e.message}")
            }
    }

    private fun uploadImagesStorage(adId: String){
        //there are multiple images in imagePickedArrayList, loop to upload all
        for (i in imagePickedArrayList.indices){
            //get model from the current position of the imagePickedArrayList
            val modelImagePicked = imagePickedArrayList[i]

            //Upload image only if picked from gallery/camera
            if (!modelImagePicked.fromInternet){

                //for name of the image in firebase storage
                val imageName = modelImagePicked.id
                //path and name of the image in firebase storage
                val filePathAndName = "Ads/$imageName"
                val imageIndexForProgress = i + 1

                //Storage reference with filePathAndName
                val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
                storageReference.putFile(modelImagePicked.imageUri!!)
                    .addOnProgressListener {snapshot ->
                        //calculate the current progress of the image being uploaded
                        val progress = 100.0 *snapshot.bytesTransferred / snapshot.totalByteCount
                        Log.d(TAG, "uploadImagesStorage: progress: $progress")
                        val message = "Uploading $imageIndexForProgress of ${imagePickedArrayList.size} images... Progress ${progress.toInt()}"
                        Log.d(TAG, "uploadImagesStorage: message: $message")

                        progressDialog.setMessage(message)
                        progressDialog.show()
                    }
                    .addOnSuccessListener {taskSnapshot ->

                        Log.d(TAG, "uploadImagesStorage: onSuccess")
                        //image uploaded get url of uploaded image
                        val uriTask = taskSnapshot.storage.downloadUrl
                        while (!uriTask.isSuccessful);
                        val uploadedImageUrl = uriTask.result

                        if (uriTask.isSuccessful){

                            val hashMap = HashMap<String, Any>()
                            hashMap["id"] = "${modelImagePicked.id}"
                            hashMap["imageUrl"] = "$uploadedImageUrl"
                            //add in firebase db. Ads -> AdId -> Images -> ImageId > ImageData
                            val ref = FirebaseDatabase.getInstance().getReference("Ads")
                            ref.child(adId).child("Images")
                                .child(imageName)
                                .updateChildren(hashMap)
                        }

                        progressDialog.dismiss()
                    }
                    .addOnFailureListener {e ->
                        //failed to upload image
                        Log.e(TAG, "uploadImagesStorage: ", e)
                        progressDialog.dismiss()
                    }
            }


        }
    }


    private fun loadAdDetails(){
        Log.d(TAG, "loadAdDetails: ")
        //Ad's db path to get the Ad details. Ads > AdId
        val ref = FirebaseDatabase.getInstance().getReference("Ads")
        ref.child(adIdForEditing)
            .addListenerForSingleValueEvent(object: ValueEventListener{

                override fun onDataChange(snapshot: DataSnapshot) {
                    //get the Ad details from firebase db, spellings should be same as in firebase db
                    val brand = "${snapshot.child("brand").value}"
                    val category = "${snapshot.child("category").value}"
                    val condition = "${snapshot.child("condition").value}"
                    latitude = (snapshot.child("latitude").value as Double) ?: 0.0
                    longitude = (snapshot.child("longitude").value as Double) ?: 0.0
                    val address = "${snapshot.child("address").value}"
                    val price = "${snapshot.child("price").value}"
                    val title = "${snapshot.child("title").value}"
                    val description = "${snapshot.child("description").value}"

                    //set data to UI Views (Form)
                    binding.brandEt.setText(brand)
                    binding.categoryAct.setText(category)
                    binding.conditionAct.setText(condition)
                    binding.locationAct.setText(address)
                    binding.priceEt.setText(price)
                    binding.titleEt.setText(title)
                    binding.descriptionEt.setText(description)

                    //Load the Ad images.Ads > AdId > Images
                    val refImages = snapshot.child("Images").ref
                    refImages.addListenerForSingleValueEvent(object: ValueEventListener{

                        override fun onDataChange(snapshot: DataSnapshot) {
                            //might be multiple images so loop to get all
                            for (ds in snapshot.children){
                                //get image data i.e. id, imageUrl. Note: Spellings should be same as in firebase db
                                val id = "${ds.child("id").value}"
                                val imageUrl = "${ds.child("imageUrl").value}"
                                //setup modelImagePicked with data we got and add to our images list i.e. imagePickedArrayList
                                val modelImagePicked = ModelImagePicked(id, null, imageUrl, true)
                                imagePickedArrayList.add(modelImagePicked)
                            }
                            //reload images (all images picked from device storage and got from firebase storage)
                            loadImages()
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }

                    })
                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

}