package com.technifysoft.olxkotlin.activities

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.technifysoft.olxkotlin.Utils
import com.technifysoft.olxkotlin.databinding.ActivityChangePasswordBinding

class ChangePasswordActivity : AppCompatActivity() {

    //View Binding
    private lateinit var binding: ActivityChangePasswordBinding


    private companion object {
        //TAG to show logs in logcat
        private const val TAG = "CHANGE_PASSWORD_TAG"
    }

    //ProgressDialog to show while sending password recovery instructions
    private lateinit var progressDialog: ProgressDialog

    //Firebase Auth for auth related tasks
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //activity_change_password.xml = ActivityChangePasswordBinding
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //init/setup ProgressDialog to show sending password recovery instructions
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait...")
        progressDialog.setCanceledOnTouchOutside(false)

        //get instance of firebase auth for Auth related tasks
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseUser = firebaseAuth.currentUser!!

        //handle toolbarBackBtn click, go-back
        binding.toolbarBackBtn.setOnClickListener {
            onBackPressed()
        }

        //handle submitBtn click, validate data to change password
        binding.submitBtn.setOnClickListener {
            validateData()
        }

    }


    private var currentPassword = ""
    private var newPassword = ""
    private var confirmNewPassword = ""

    private fun validateData(){
        //input data
        currentPassword = binding.currentPasswordEt.text.toString().trim()
        newPassword = binding.newPasswordEt.text.toString().trim()
        confirmNewPassword = binding.confirmNewPasswordEt.text.toString().trim()

        Log.d(TAG, "validateData: currentPassword: $currentPassword")
        Log.d(TAG, "validateData: newPassword: $newPassword")
        Log.d(TAG, "validateData: confirmNewPassword: $confirmNewPassword")

        //validate data
        if (currentPassword.isEmpty()){
            //Current Password Field (currentPasswordEt) is empty, show error in currentPasswordEt
            binding.currentPasswordEt.error = "Enter current password!"
            binding.currentPasswordEt.requestFocus()
        } else if (newPassword.isEmpty()){
            //New Password Field (newPasswordEt) is empty, show error in newPasswordEt
            binding.newPasswordEt.error = "Enter new password!"
            binding.newPasswordEt.requestFocus()
        } else if (confirmNewPassword.isEmpty()){
            //Confirm New Password Field (confirmNewPasswordEt) is empty, show error in confirmNewPasswordEt
            binding.confirmNewPasswordEt.error = "Enter confirm new password!"
            binding.confirmNewPasswordEt.requestFocus()
        } else if (newPassword != confirmNewPassword){
            //password in newPasswordEt & confirmNewPasswordEt doesn't match, show error in confirmNewPasswordEt
            binding.confirmNewPasswordEt.error = "Password doesn't match"
            binding.confirmNewPasswordEt.requestFocus()
        } else {
            //all data is validated, verify current password is correct first before updating password
            authenticateUserForUpdatePassword()
        }

    }

    private fun authenticateUserForUpdatePassword(){
        //show progress
        progressDialog.setMessage("Authenticating User")
        progressDialog.show()

        //before changing password re-authenticate the user to check if the user has entered correct current password
        val authCredential = EmailAuthProvider.getCredential(firebaseUser.email.toString(), currentPassword)
        firebaseUser.reauthenticate(authCredential)
            .addOnSuccessListener {
                //successfully authenticated, begin update
                Log.d(TAG, "authenticateUserForUpdatePassword: Auth success")
                updatePassword()
            }
            .addOnFailureListener { e ->
                //failed to authenticate user, maybe wrong current password entered
                Log.e(TAG, "authenticateUserForUpdatePassword: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to authenticate due to ${e.message}")
            }

    }

    private fun updatePassword(){
        //show progress
        progressDialog.setMessage("Changing Password")
        progressDialog.show()

        //begin update password, pass the new password as parameter
        firebaseUser.updatePassword(newPassword)
            .addOnSuccessListener {
                //password update success, you may do logout and move to login activity if you want
                Log.d(TAG, "updatePassword: Password updated...")
                progressDialog.dismiss()
                Utils.toast(this, "Password updated...!")
            }
            .addOnFailureListener { e ->
                //password update failure, show error message
                Log.e(TAG, "updatePassword: ", e)
                progressDialog.dismiss()
                Utils.toast(this, "Failed to update password due to ${e.message}")
            }
    }
}