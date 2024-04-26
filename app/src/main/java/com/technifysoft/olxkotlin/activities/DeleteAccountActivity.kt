package com.technifysoft.olxkotlin.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.ActivityDeleteAccountBinding

class DeleteAccountActivity : AppCompatActivity() {

    //View Binding
    private lateinit var binding: ActivityDeleteAccountBinding

    companion object {
        //TAG for logs in logcat
        private const val TAG = "DELETE_ACCOUNT_TAG"
    }

    //ProgressDialog to show while deleting account
    private lateinit var progressDialog: ProgressDialog

    //FirebaseAuth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth
    //FirebaseUser to get current user and delete
    private var firebaseUser: FirebaseUser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //init view binding... activity_delete_account.xml = ActivityDeleteAccountBinding
        binding = ActivityDeleteAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init/setup ProgressDialog to show while deleting account
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //get instance of FirebaseAuth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()
        //get instance of FirebaseUser to get current user and delete
        firebaseUser = firebaseAuth.currentUser

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle submitBtn click, start account deletion
        binding.submitBtn.setOnClickListener {
            deleteAccount()
        }

    }

    private fun deleteAccount(){
        Log.d(TAG, "deleteAccount: ")

        //show progress
        progressDialog.setMessage("Deleting User Account")
        progressDialog.show()

        //uid of currently signed-in user. get it before deleting user from Firebase Authentication since we need that uid to delete user data from firebase db
        val myUid = firebaseAuth.uid

        //Step 1: Delete User Account i.e. from accounts in Firebase Authentication
        firebaseUser!!.delete()
            .addOnSuccessListener {
                //User account deleted
                Log.d(TAG, "deleteAccount: Account deleted...")

                progressDialog.setMessage("Deleting User Ads")

                //Step 2: Remove User Ads, Currently We have not worked on Ads, Ads will be saved in DB > Ads > AdId. each Ad will contain uid of the owner
                val refUserAds = FirebaseDatabase.getInstance().getReference("Ads")
                refUserAds.orderByChild("uid").equalTo(myUid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            //there maybe multiple ads by user need to loop
                            for (ds in snapshot.children){
                                //delete Ad
                                ds.ref.removeValue()
                            }
                            progressDialog.setMessage("Deleting User Data")

                            //Step 3: Remove User Data, DB > Users > UserId
                            val refUsers = FirebaseDatabase.getInstance().getReference("Users")
                            refUsers.child(myUid!!).removeValue()
                                .addOnSuccessListener {
                                    //Account data deleted from firebase db

                                    Log.d(TAG, "onDataChange: User Data deleted")
                                    progressDialog.dismiss()
                                    startMainActivity()
                                }
                                .addOnFailureListener {e ->
                                    //Failed to delete user data, maybe due to firebase db rules, we have to make it public since we delete data after account deletion
                                    Log.e(TAG, "onDataChange: ", e)
                                    progressDialog.dismiss()
                                    Utils.toast(
                                        this@DeleteAccountActivity,
                                        "Failed to delete user data due to ${e.message}"
                                    )
                                    startMainActivity()
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })
            }
            .addOnFailureListener {e ->
                //failed to delete user account, maybe user need to re-login for authentication purpose before deleting
                Log.e(TAG, "deleteAccount: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to delete account due to ${e.message}")
            }
    }

    private fun startMainActivity(){
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity() //clear back-stack of activities
    }

    override fun onBackPressed() {
        startMainActivity()
    }
}