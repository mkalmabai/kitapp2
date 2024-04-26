package com.technifysoft.olxkotlin.fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.technifysoft.olxkotlin.activities.ChangePasswordActivity
import com.technifysoft.olxkotlin.activities.DeleteAccountActivity
import com.technifysoft.olxkotlin.activities.MainActivity
import com.technifysoft.olxkotlin.activities.ProfileEditActivity
import com.technifysoft.olxkotlin.R
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.FragmentAccountBinding

class AccountFragment : Fragment() {

    private lateinit var binding: FragmentAccountBinding

    private companion object{
        private const val TAG = "ACCOUNT_TAG"
    }

    //Context for this fragment class
    private lateinit var mContext: Context

    //ProgressDialog to show while account verification
    private lateinit var progressDialog: ProgressDialog

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onAttach(context: Context) {
        //get and init the context for this fragment class
        mContext = context
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentAccountBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //init/setup ProgressDialog to show while account verification
        progressDialog = ProgressDialog(mContext)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()

        loadMyInfo()

        //handle logoutBtn click, logout user and start MainActivity
        binding.logoutCv.setOnClickListener {
            firebaseAuth.signOut()//logout user
            //start MainActivity
            startActivity(Intent(mContext, MainActivity::class.java))
            activity?.finishAffinity()
        }

        //handle editProfileCv click, start ProfileEditActivity
        binding.editProfileCv.setOnClickListener {
            startActivity(Intent(mContext, ProfileEditActivity::class.java))
        }

        //handle changePasswordCv click, start ChangePasswordActivity
        binding.changePasswordCv.setOnClickListener {
            startActivity(Intent(mContext, ChangePasswordActivity::class.java))
        }

        //handle verifyAccountCv click, start user verification
        binding.verifyAccountCv.setOnClickListener {
            verifyAccount()
        }

        //handle deleteAccountCv click, start DeleteAccountActivity
        binding.deleteAccountCv.setOnClickListener {
            startActivity(Intent(mContext, DeleteAccountActivity::class.java))
        }

    }

    private fun loadMyInfo(){
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
                    var timestamp = "${snapshot.child("timestamp").value}"
                    val userType = "${snapshot.child("userType").value}"

                    //concatenate phone code and phone number to make full phone number
                    val phone = phoneCode+phoneNumber

                    //to avoid null or format exceptions
                    if (timestamp == "null"){
                        timestamp = "0"
                    }

                    //format timestamp to dd/MM/yyyy
                    val formattedDate = Utils.formatTimestampDate(timestamp.toLong())

                    //set data to UI
                    binding.emailTv.text = email
                    binding.nameTv.text = name
                    binding.dobTv.text = dob
                    binding.phoneTv.text = phone
                    binding.memberSinceTv.text = formattedDate

                    //check user type i.e. Email/Phone/Google In case of Phone & Google account is already verified but in case of Email account user have to verify
                    if (userType == "Email") {
                        //userType is Email, have to check if verified or not
                        val isVerified = firebaseAuth.currentUser!!.isEmailVerified
                        if (isVerified) {
                            //Verified, hide the Verify Account option
                            binding.verifyAccountCv.visibility = View.GONE
                            binding.verificationTv.text = "Verified"
                        } else {
                            //Not verified, show the Verify Account option
                            binding.verifyAccountCv.visibility = View.VISIBLE
                            binding.verificationTv.text = "Not Verified"
                        }
                    } else {
                        //userType is Google or Phone, no need to check if verified or not as it is already verified, hide the Verify Account option
                        binding.verifyAccountCv.visibility = View.GONE
                        binding.verificationTv.text = "Verified"
                    }

                    //Set profile image to profileIv
                    try {
                        Glide.with(mContext)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_white)
                            .into(binding.profileIv)
                    } catch (e: Exception) {
                        //Failed to get/set image show exception in log
                        Log.e(TAG, "onDataChange: ", e)
                    }

                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun verifyAccount(){
        Log.d(TAG, "verifyAccount: ")
        //show progress
        progressDialog.setMessage("Sending account verification instructions to your email...")
        progressDialog.show()

        //send account/email verification instructions to the registered email.
        firebaseAuth.currentUser!!.sendEmailVerification()
            .addOnSuccessListener {
                //Account verification instructions are sent to the currently signed in user, sometimes it goes to spam folder so make sure to check it too
                Log.d(TAG, "verifyAccount: Successfully sent")
                progressDialog.dismiss()
                Utils.toast(mContext, "Account verification instructions sent to your email...")
            }
            .addOnFailureListener {e ->
                //failed to send the account verification instructions
                Log.e(TAG, "verifyAccount: ", e)
                progressDialog.dismiss()
                Utils.toast(mContext, "Failed to send due to ${e.message}")
            }
    }

}