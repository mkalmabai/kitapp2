package com.technifysoft.olxkotlin.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import com.google.firebase.database.FirebaseDatabase
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.ActivityLoginPhoneBinding
import java.util.concurrent.TimeUnit

class LoginPhoneActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginPhoneBinding

    private companion object{
        //Tag to show logs
        private const val TAG = "PHONE_LOGIN_TAG"
    }

    //ProgressDialog to show while phone login
    private lateinit var progressDialog: ProgressDialog

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    private var forceRefreshingToken: ForceResendingToken? = null

    private lateinit var mCallbacks: OnVerificationStateChangedCallbacks

    private var mVerificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //For the start show phone input UI and hide OTP UI
        binding.phoneInputRl.visibility = View.VISIBLE
        binding.otpInputRl.visibility = View.GONE

        //init/setup ProgressDialog to show while sign-up
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //Firebase Auth for auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()

        //listen for phone login callbacks. Hint: you may put here instead of creating a function
        phoneLoginCallBacks()

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle sendOtpBtn send OTP to input phone number
        binding.sendOtpBtn.setOnClickListener {
            validateData()
        }

        //handle resendOtpTv re send OTP to input phone number
        binding.resendOtpTv.setOnClickListener {
            resendVerificationCode(forceRefreshingToken)
        }

        //handle verifyOtpBtn verify the OTP sent to input phone number
        binding.verifyOtpBtn.setOnClickListener {
            //input OTP
            val otp = binding.otpEt.text.toString().trim()
            Log.d(TAG, "onCreate: otp: $otp")
            //validate OTP
            if (otp.isEmpty()) {
                //OTP is empty, show error in otpEt
                binding.otpEt.error = "Enter OTP"
                binding.otpEt.requestFocus()
            } else if (otp.length < 6) {
                //OTP length is less then 6, show error in otpEt
                binding.otpEt.error = "OTP length must be 6 characters long"
                binding.otpEt.requestFocus()
            } else {
                verifyPhoneNumberWithCode(mVerificationId, otp)
            }
        }

    }

    private var phoneCode = ""
    private var phoneNumber = ""
    private var phoneNumberWithCode = ""

    private fun validateData(){
        //input data
        phoneCode = binding.phoneCodeTil.selectedCountryCodeWithPlus
        phoneNumber = binding.phoneNumberEt.text.toString().trim()
        phoneNumberWithCode = phoneCode + phoneNumber

        Log.d(TAG, "validateData: phoneCode: $phoneCode")
        Log.d(TAG, "validateData: phoneNumber: $phoneNumber")
        Log.d(TAG, "validateData: phoneNumberWithCode: $phoneNumberWithCode")
        //validate data
        if (phoneNumber.isEmpty()){
            //Phone number not entered, show error to phoneNumberEt
            binding.phoneNumberEt.error = "Enter Phone Number"
            binding.phoneNumberEt.requestFocus()
        } else {
            startPhoneNNumberVerification()
        }
    }

    private fun startPhoneNNumberVerification(){
        Log.d(TAG, "startPhoneNNumberVerification: ")
        //show progress
        progressDialog.setMessage("Sending OTP to $phoneNumberWithCode")
        progressDialog.show()
        //setup phone auth options
        val options = PhoneAuthOptions.newBuilder(firebaseAuth) //FirebaseAuth instance
            .setPhoneNumber(phoneNumberWithCode) //Phone Number with country code e.g. +92*********
            .setTimeout(60L, TimeUnit.SECONDS) //Timeout and unit
            .setActivity(this)  //Activity (for callback binding)
            .setCallbacks(mCallbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private fun phoneLoginCallBacks(){
        Log.d(TAG, "phoneLoginCallBacks: ")

        mCallbacks = object: OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: ")
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ", e)
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.

                progressDialog.dismiss()

                Utils.toast(this@LoginPhoneActivity, "${e.message}")

            }

            override fun onCodeSent(verificationId: String, token: ForceResendingToken) {
                Log.d(TAG, "onCodeSent: verificationId: $verificationId")
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId
                forceRefreshingToken = token
                //OTP is sent so hide progress for now
                progressDialog.dismiss()
                //OTP is sent so hide phone ui and show otp ui
                binding.phoneInputRl.visibility = View.GONE
                binding.otpInputRl.visibility = View.VISIBLE

                //Show toast for success sending OTP
                Utils.toast(this@LoginPhoneActivity, "OTP is sent to $phoneNumberWithCode")
                //show user a message that Please type the verification code sent to the phone number user has input
                binding.loginLabelTv.text = "Please type the verification code sent to $phoneNumberWithCode"

            }

            override fun onCodeAutoRetrievalTimeOut(p0: String) {




            }
        }
    }


    private fun verifyPhoneNumberWithCode(verificationId: String?, otp: String) {
        Log.d(TAG, "verifyPhoneNumberWithCode: verificationId: $verificationId")
        Log.d(TAG, "verifyPhoneNumberWithCode: otp: $otp")
        //show progress
        progressDialog.setMessage("Verifying OTP")
        progressDialog.show()
        //PhoneAuthCredential with verification id and OTP to signIn user with signInWithPhoneAuthCredential
        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
        signInWithPhoneAuthCredential(credential)
    }

    private fun resendVerificationCode(token: ForceResendingToken?){
        Log.d(TAG, "resendVerificationCode: ")

        //show progress
        progressDialog.setMessage("Resending OTP to $phoneNumberWithCode")
        progressDialog.show()
        //setup phone auth options
        val options = PhoneAuthOptions.newBuilder(firebaseAuth) //FirebaseAuth instance
            .setPhoneNumber(phoneNumberWithCode) //Phone Number with country code e.g. +92*********
            .setTimeout(60L, TimeUnit.SECONDS) //Timeout and unit
            .setActivity(this)  //Activity (for callback binding)
            .setCallbacks(mCallbacks)
            .setForceResendingToken(token!!)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        Log.d(TAG, "signInWithPhoneAuthCredential: ")
        //show progress
        progressDialog.setMessage("Logging In")
        progressDialog.show()

        //SignIn in to firebase auth using Phone Credentials
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                Log.d(TAG, "signInWithPhoneAuthCredential: Success")
                //firebase auth with phone credential is succeed, let's check if user is new or existing
                if (authResult.additionalUserInfo!!.isNewUser){
                    Log.d(TAG, "signInWithPhoneAuthCredential: New User, Account Created")
                    //New User, Account created. Let's save user info to firebase realtime database
                    updateUserInfoDb()
                }
                else{
                    Log.d(TAG, "signInWithPhoneAuthCredential: Existing User, Logged In")
                    //Existing User, Signed In. No need to save user info to firebase realtime database, Start MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }
            .addOnFailureListener {e->
                //firebase auth with phone credential is failed, show exception
                Log.e(TAG, "signInWithPhoneAuthCredential: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to login due to ${e.message}")
            }
    }

    private fun updateUserInfoDb(){
        Log.d(TAG, "updateUserInfoDb: ")
        //show progress
        progressDialog.setMessage("Saving User Info")
        progressDialog.show()
        //Let's save user info to Firebase Realtime database key names should be same as we done in Register User via email and Google
        //get current timestamp e.g. to show user registration date/time
        val timestamp = Utils.getTimestamp()
        val registeredUserUid = firebaseAuth.uid

        //setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        val hashMap = HashMap<String, Any?>()
        hashMap["name"] = ""
        hashMap["phoneCode"] = "$phoneCode"
        hashMap["phoneNumber"] = "$phoneNumber"
        hashMap["profileImageUrl"] = ""
        hashMap["dob"] = ""
        hashMap["userType"] = "Phone" //possible values Email/Phone/Google
        hashMap["typingTo"] = ""
        hashMap["timestamp"] = timestamp
        hashMap["onlineStatus"] = true
        hashMap["email"] = ""
        hashMap["uid"] = registeredUserUid

        //set data to firebase db
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(registeredUserUid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                //User info save success
                Log.d(TAG, "updateUserInfoDb: User info saved")
                progressDialog.dismiss()
                //Start MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            }
            .addOnFailureListener {e ->
                //User info save failed
                Log.e(TAG, "updateUserInfoDb: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to save user info due to ${e.message}")
            }
    }

}