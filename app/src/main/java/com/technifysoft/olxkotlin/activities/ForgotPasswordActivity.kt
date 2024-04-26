package com.technifysoft.olxkotlin.activities

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    //View Binding
    private lateinit var binding: ActivityForgotPasswordBinding


    private companion object {
        //TAG to show logs in logcat
        private const val TAG = "FORGOT PASSWORD"
    }

    //ProgressDialog to show while sending password recovery instructions
    private lateinit var progressDialog: ProgressDialog

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //activity_forgot_password.xml = ActivityForgotPasswordBinding
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init/setup ProgressDialog to show sending password recovery instructions
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle submitBtn click, validate data to start password recovery
        binding.submitBtn.setOnClickListener {
            validateData()
        }

    }


    private var email = ""

    private fun validateData(){
        //input data
        email = binding.emailEt.text.toString().trim()

        Log.d(TAG, "validateData: email: $email")

        //validate data
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            //invalid email pattern, show error in emailEt
            binding.emailEt.error = "Invalid Email Pattern!"
            binding.emailEt.requestFocus()
        } else {
            //email pattern is valid, send password recovery instructions
            sendPasswordRecoveryInstructions()
        }
    }

    private fun sendPasswordRecoveryInstructions(){
        Log.d(TAG, "sendPasswordRecoveryInstructions: ")

        //show progress
        progressDialog.setMessage("Sending password reset instructions to $email")
        progressDialog.show()

        //send password recovery instructions, pass the input email as param
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                //instructions sent, check email, sometimes it goes in spam folder so if not in inbox check your spam folder, You may move back to LoginEmailActivity
                progressDialog.dismiss()
                Utils.toast(this, "Instructions to reset password sent to $email")
            }
            .addOnFailureListener { e ->
                //failed to send instructions
                Log.e(TAG, "sendPasswordRecoveryInstructions: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to send due to ${e.message}")
            }
    }
}