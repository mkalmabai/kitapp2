package com.technifysoft.olxkotlin.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.technifysoft.olxkotlin.fragments.HomeFragment
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.ActivityMainBinding
import com.technifysoft.olxkotlin.fragments.AccountFragment
import com.technifysoft.olxkotlin.fragments.ChatsFragment
import com.technifysoft.olxkotlin.fragments.MyAdsFragment

class MainActivity : AppCompatActivity() {

    //view binding or respective layout i.e. activity_main.xml
    private lateinit var binding: ActivityMainBinding

    companion object  {
        private const val TAG = "MAIN_TAG"
    }

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //activity_main.xml = ActivityMainBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()
        //check if user is logged in or not
        if (firebaseAuth.currentUser == null){
            //user is not logged in, move to LoginOptionsActivity
            startLoginOptions()
        } else {
            //User Logged-In, ask notification permission and update FCM Token
            updateFcmToken()
            askNotificationPermission()
        }

        //By default (when app open) show HomeFragment
        showHomeFragment()

        //handle bottomNv item clicks to navigate between fragments
        binding.bottomNv.setOnItemSelectedListener {item ->

            when(item.itemId){
                R.id.menu_home -> {
                    //Home item clicked, show HomeFragment
                    showHomeFragment()

                    true
                }
                R.id.menu_chats -> {
                    //Chats item clicked, show ChatsFragment
                    if (firebaseAuth.currentUser == null) {
                        Utils.toast(this, "Login Required")
                        startLoginOptions()

                        false
                    } else {
                        showChatsFragment()

                        true
                    }
                }
                R.id.menu_my_ads -> {
                    //My Ads item clicked, show MyAdsFragment
                    if (firebaseAuth.currentUser == null) {
                        Utils.toast(this, "Login Required")
                        startLoginOptions()

                        false
                    } else {
                        showMyAdsFragment()

                        true
                    }
                }
                R.id.menu_account -> {
                    //Account item clicked, show AccountFragment
                    if (firebaseAuth.currentUser == null) {
                        Utils.toast(this, "Login Required")
                        startLoginOptions()

                        false
                    } else {
                        showAccountFragment()

                        true
                    }
                }
                else -> {
                    false
                }
            }

        }

        //handle sellFab click, start AdCreateActivity to add create a new Ad
        binding.sellFab.setOnClickListener {
            val intent = Intent(this, AdCreateActivity::class.java)
            intent.putExtra("isEditMode", false)
            startActivity(intent)
        }
    }

    private fun showHomeFragment(){
        //change toolbar textView text/title to Home
        binding.toolbarTitleTv.text = "Home"

        //Show HomeFragment
        val fragment = HomeFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentsFl.id, fragment, "HomeFragment")
        fragmentTransaction.commit()
    }

    private fun showChatsFragment(){
        //change toolbar textView text/title to Chats
        binding.toolbarTitleTv.text = "Chats"

        //Show ChatsFragment
        val fragment = ChatsFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentsFl.id, fragment, "ChatsFragment")
        fragmentTransaction.commit()
    }

    private fun showMyAdsFragment(){
        //change toolbar textView text/title to My Ads
        binding.toolbarTitleTv.text = "My Ads"

        //Show MyAdsFragment
        val fragment = MyAdsFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentsFl.id, fragment, "MyAdsFragment")
        fragmentTransaction.commit()
    }

    private fun showAccountFragment(){
        //change toolbar textView text/title to Account
        binding.toolbarTitleTv.text = "Account"

        //Show AccountFragment
        val fragment = AccountFragment()
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(binding.fragmentsFl.id, fragment, "AccountFragment")
        fragmentTransaction.commit()
    }

    private fun startLoginOptions(){
        startActivity(Intent(this, LoginOptionsActivity::class.java))
    }


    private fun updateFcmToken(){
        val myUid = "${firebaseAuth.uid}"
        Log.d(TAG, "updateFcmToken: ")
        //1) Get FCM Token
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener {fcmToken ->
                //Firebase FCM Token get success
                Log.d(TAG, "updateFcmToken: fcmToken $fcmToken")
                //Setup Data (fcmToken) to update to currently logged-in user's db
                val hashMap  = HashMap<String, Any>()
                hashMap["fcmToken"] = "$fcmToken"

                //2) Update FCM Token to Firebase DB
                val ref = FirebaseDatabase.getInstance().getReference("Users")
                ref.child(myUid)
                    .updateChildren(hashMap)
                    .addOnSuccessListener {
                        //FCM Token Update to db success
                        Log.d(TAG, "updateFcmToken: FCM Token Update to db success")
                    }
                    .addOnFailureListener {e->
                        //FCM Token Update to db failed
                        Log.e(TAG, "updateFcmToken: ", e)
                    }
            }
            .addOnFailureListener {e  ->
                //Firebase FCM Token get failed
                Log.e(TAG, "updateFcmToken: ", e)
            }


    }

    private fun askNotificationPermission(){
        //Android 13/TIRAMISU/API_33 and above requires POST_NOTIFICATIONS permission e.g. to show push notifications
        if (Build.VERSION.SDK_INT  >= Build.VERSION_CODES.TIRAMISU){
            //Check if permission is granted or not
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_DENIED) {  //Permission not granted yet, Request
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        //Handle result of permission allowed/denied after requesting
        Log.d(TAG, "requestNotificationPermission: isGranted: $isGranted")
    }

}

/*Steps
 * 1- Add FCM (Firebase Cloud Messaging) library. Update Project & other Libraries
 * 2- Update FCM Token whenever app starts
 * 3- Ask Notification permission requires for Android 13 and above
 * 4- Send Chat Notification
 * 5- Handle/Show Chat Notification
 * 6- Testing*/