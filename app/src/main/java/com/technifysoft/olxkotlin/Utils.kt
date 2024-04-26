package com.technifysoft.olxkotlin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import kotlin.collections.HashMap

/*A class that will contain static functions, constants, variables that we will be used in whole application*/
object Utils {

    const val MESSAGE_TYPE_TEXT = "TEXT"
    const val MESSAGE_TYPE_IMAGE = "IMAGE"

    //constants to define possible Ads status. When ad is published the Ad status will be set AVAILABLE in firebase db. so user can mark as SOLD later when it is sold
    const val AD_STATUS_AVAILABLE = "AVAILABLE"
    const val AD_STATUS_SOLD = "SOLD"

    const val NOTIFICATION_TYPE_NEW_MESSAGE = "NOTIFICATION_TYPE_NEW_MESSAGE"
    //TODO Change FCM SERVER KEY
    const val FCM_SERVER_KEY = "AAAAkWsijz4:APA91bFQZXtgkYf_vyJb5GodsrY6z845aJx7NhJL2BQ3WOA-2MGnesI5nP0I4uw2uk7asaZiblcdvhdUy7ddfd4u6FEphnP15uQ8O3LwQxh-n_g0OC3T__06ph-C-4pwNhatJ7Ah77yd"

    //Categories array of the Ads
    val categories = arrayOf(
        "All",
        "Mobiles",
        "Computer/Laptop",
        "Electronics & Home Appliances",
        "Vehicles",
        "Furniture & Home Decor",
        "Fashion & Beauty",
        "Books",
        "Sports",
        "Animals",
        "Businesses",
        "Agriculture"
    )

    //Categories icon array of Ads
    val categoryIcons = arrayOf(
        R.drawable.ic_category_all,
        R.drawable.ic_category_mobiles,
        R.drawable.ic_category_computer,
        R.drawable.ic_category_electronics,
        R.drawable.ic_category_vehicles,
        R.drawable.ic_category_furniture,
        R.drawable.ic_category_fashion,
        R.drawable.ic_category_books,
        R.drawable.ic_category_sports,
        R.drawable.ic_category_animals,
        R.drawable.ic_category_business,
        R.drawable.ic_category_agriculture
    )


    //Ad product conditions e.g. New, Used, Refurbished
    val conditions = arrayOf(
        "New",
        "Used",
        "Refurbished"
    )

    /** A Function to show Toast
     * @param context the context of activity/fragment from where this function will be called
     * @param message the message to be shown in the Toast
     */
    fun toast(context: Context, message: String){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /** A Function to get current timestamp
     * @return Return the current timestamp as long datatype
     */
    fun getTimestamp() : Long{

        return System.currentTimeMillis()
    }

    /** A Function format timestamp to date dd/MM/yyyy
    @param timestamp the timestamp of type Long that we need to format to dd/MM/yyyy
    @return timestamp formatted to date dd/MM/yyyy*/
    fun formatTimestampDate(timestamp: Long) : String{

        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = timestamp

        return DateFormat.format("dd/MM/yyyy", calendar).toString()
    }

    /** A Function to format timestamp to date and time e.g. dd/MM/yyyy hh:mm:a
     * @param timestamp the timestamp of type Long that we need to format to dd/MM/yyyy hh:mm:a
     * @return timestamp formatted to date dd/MM/yyyy hh:mm:a
     */
    fun formatTimestampDateTime(timestamp: Long) : String{

        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = timestamp

        return DateFormat.format("dd/MM/yyyy hh:mm:a", calendar).toString()
    }

    /**
     * Generate Chat Path
     * This will generate chat path by sorting these UIDs and concatenate sorted array of UIDs having _ in between
     * All messages of these 2  users will be saved in this path
     *
     * @param receiptUid The UID of the receipt
     * @param yourUid    The UID of the current logged-in user
     */
    fun chatPath(receiptUid: String, yourUid: String): String {
        //Array of UIDs
        val arrayUids = arrayOf(receiptUid, yourUid)
        //Sort Array
        Arrays.sort(arrayUids)
        //Concatenate both UIDs (after sorting) having _ between
        //return chat path e.g.  if receiptUid = mfVrv1c1U6goV5sbHjvXpn2moUj1 and yourUid = hQknm8IBoAZkqUqkPDzPTK4UzBX2 then chatPath = hQknm8IBoAZkqUqkPDzPTK4UzBX2_mfVrv1c1U6goV5sbHjvXpn2moUj1
        return "${arrayUids[0]}_${arrayUids[1]}"
    }


    /**
     * Add the add to favorite
     *
     * @param context the context of activity/fragment from where this function will be called
     * @param adId    the Id of the add to be added to favorite of current user
     */
    fun addToFavorite(context: Context, adId: String){
        //we can add only if user is logged in
        //1)Check if user is logged in
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null){
            //not logged in, can't add to favorite
            Utils.toast(context, "You're not logged-in!")
        } else {
            //logged in, can add to favorite
            //get timestamp
            val timestamp = Utils.getTimestamp()

            //setup data to add in firebase database
            val hashMap = HashMap<String, Any>()
            hashMap["adId"] = adId
            hashMap["timestamp"] = timestamp

            //Add data to db. Users > uid > Favorites > adId > favoriteDataObj
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(adId)
                .setValue(hashMap)
                .addOnSuccessListener {
                    //success
                    Utils.toast(context, "Added to favorite..!")
                }
                .addOnFailureListener { e->
                    //failure
                    Utils.toast(context, "Failed to add to favorite due to ${e.message}")
                }
        }
    }

    /**
     * Remove the add from favorite
     *
     * @param context the context of activity/fragment from where this function will be called
     * @param adId    the Id of the add to be removed from favorite of current user
     */
    fun removeFromFavorite(context: Context, adId: String){
        //we can add only if user is logged in
        //1)Check if user is logged in
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null){
            //not logged in, can't remove from favorite
            Utils.toast(context, "You're not logged-in!")
        } else {
            //logged in, can remove from favorite //Remove data from db. Users > uid > Favorites > adId
            val ref = FirebaseDatabase.getInstance().getReference("Users")
            ref.child(firebaseAuth.uid!!).child("Favorites").child(adId)
                .removeValue()
                .addOnSuccessListener {
                    //Success
                    Utils.toast(context, "Removed from favorite!")
                }
                .addOnFailureListener { e->
                    //Failed
                    Utils.toast(context, "Failed to remove from favorite due to ${e.message}")
                }
        }
    }
    /**
     * Launch Call Intent with phone number
     *
     * @param context the context of activity/fragment from where this function will be called
     * @param phone   the phone number that will be opened in call intent
     */
    fun callIntent(context: Context, phone: String){
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tel:${Uri.encode(phone)}"))
        context.startActivity(intent)
    }

    /**
     * Launch Sms Intent with phone number
     *
     * @param context the context of activity/fragment from where this function will be called
     * @param phone   the phone number that will be opened in sms intent
     */
    fun smsIntent(context: Context, phone: String){
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${Uri.encode(phone)}"))
        context.startActivity(intent)
    }

    /**
     * Launch Google Map with input location
     *
     * @param context the context of activity/fragment from where this function will be called
     * @param latitude the latitude of the location to be shown in google map
     * @param longitude the longitude of the location to be shown in google map
     */
    fun mapIntent(context: Context, latitude: Double, longitude: Double){
        // Create a Uri from an intent string. Use the result to create an Intent.
        val gmmIntentUri = Uri.parse("http://maps.google.com/maps?daddr=$latitude,$longitude")

        // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        // Make the Intent explicit by setting the Google Maps package
        mapIntent.setPackage("com.google.android.apps.maps")
        // Attempt to start an activity that can handle the Intent e.g. Google Map
        if (mapIntent.resolveActivity(context.packageManager) != null){
            //Google Map installed, start
            context.startActivity(mapIntent)
        } else {
            //Google Map not installed, can't start
            Utils.toast(context, "Google Map not installed!")
        }
    }

}