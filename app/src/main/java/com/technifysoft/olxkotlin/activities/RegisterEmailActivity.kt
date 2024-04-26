package com.technifysoft.olxkotlin.activities

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.ActivityRegisterEmailBinding

class RegisterEmailActivity : AppCompatActivity() {


    private lateinit var binding: ActivityRegisterEmailBinding


    private companion object{
        private const val TAG = "REGISTER_TAG"
    }

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    //ProgressDialog to show while sign-up
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //activity_register_email.xml = ActivityRegisterEmailBinding
        binding = ActivityRegisterEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()

        //init/setup ProgressDialog to show while sign-up
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle haveAccountTv click, go-back-to LoginEmailActivity
        binding.haveAccountTv.setOnClickListener {
            onBackPressed()
        }

        //handle registerBtn click, start user registration
        binding.registerBtn.setOnClickListener {
            validateData()
        }

    }

    private var email = ""
    private var password = ""
    private var cPassword = ""

    private fun validateData(){
        //input data
        email = binding.emailEt.text.toString().trim()
        password = binding.passwordEt.text.toString().trim()
        cPassword = binding.cPasswordEt.text.toString().trim()

        Log.d(TAG, "validateData: email :$email")
        Log.d(TAG, "validateData: password :$password")
        Log.d(TAG, "validateData: confirm password :$cPassword")

        //validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //email pattern is invalid, show error
            binding.emailEt.error = "Invalid Email Pattern"
            binding.emailEt.requestFocus()
        }
        else if (password.isEmpty()){
            //password is not entered, show error
            binding.passwordEt.error = "Enter Password"
            binding.passwordEt.requestFocus()
        }
        else if (cPassword.isEmpty()){
            //confirm password is not entered, show error
            binding.cPasswordEt.error = "Enter Confirm Password"
            binding.cPasswordEt.requestFocus()
        }
        else if (password != cPassword){
            //password and confirm password is not same, show error
            binding.cPasswordEt.error = "Password Doesn't Match"
            binding.cPasswordEt.requestFocus()
        }
        else{
            //all data is valid, start sign-up
            registerUser()
        }

    }

    private fun registerUser(){
        Log.d(TAG, "registerUser: ")
        //show progress
        progressDialog.setMessage("Creating account")
        progressDialog.show()

        //start user sign-up
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                //User Register success, We also need to save user info to firebase db
                Log.d(TAG, "registerUser: Register Success")
                updateUserInfo()
            }
            .addOnFailureListener { e ->
                //User Register failed
                Log.e(TAG, "registerUser: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to create account due to ${e.message}")
            }
    }

    private fun updateUserInfo(){
        Log.d(TAG, "updateUserInfo: ")
        //change progress dialog message
        progressDialog.setMessage("Saving User Info")

        //get current timestamp e.g. to show user registration date/time
        val timestamp = Utils.getTimestamp()
        val registeredUserEmail = firebaseAuth.currentUser!!.email //get email of registered user
        val registeredUserUid = firebaseAuth.uid //get uid of registered user

        //setup data to save in firebase realtime db. most of the data will be empty and will set in edit profile
        val hashMap = HashMap<String, Any>()
        hashMap["name"] = ""
        hashMap["phoneCode"] = ""
        hashMap["phoneNumber"] = ""
        hashMap["profileImageUrl"] = ""
        hashMap["dob"] = ""
        hashMap["userType"] = "Email" //possible values Email/Phone/Google
        hashMap["typingTo"] = ""
        hashMap["timestamp"] = timestamp
        hashMap["onlineStatus"] = true
        hashMap["email"] = "$registeredUserEmail"
        hashMap["uid"] = "$registeredUserUid"

        //set data to firebase db
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(registeredUserUid!!)
            .setValue(hashMap)
            .addOnSuccessListener {
                //Firebase db save success
                Log.d(TAG, "updateUserInfo: User registered...")
                progressDialog.dismiss()

                //Start MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()//finish current and all activities from back stack

            }
            .addOnFailureListener { e ->
                //Firebase db save failed
                Log.e(TAG, "updateUserInfo: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to save user info due to ${e.message}")
            }
    }
}