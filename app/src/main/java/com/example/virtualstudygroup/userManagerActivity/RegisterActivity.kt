package com.example.virtualstudygroup.userManagerActivity

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.virtualstudygroup.R
import com.example.virtualstudygroup.chatActivity.MessageActivity
import com.example.virtualstudygroup.getApp
import com.example.virtualstudygroup.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*


class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var selectedPhoto: Uri? = null
    private var currentUser: FirebaseUser ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onstart")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        getApp().currentUser = null
        supportActionBar?.hide()
        auth = Firebase.auth

        Toast.makeText(this, getString(R.string.hint_to_upload_picture),
                                Toast.LENGTH_SHORT).show()

        logo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        switch_to_login.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        btnSignup.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            btnSignup.isClickable = false
            val username = username_input.text.toString()
            val password = password_input.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username or password can't be empty", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword(username, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            currentUser = auth.currentUser!!
                            Log.i(TAG, "Create user success, uid=${currentUser?.uid}")
                            selectedPhoto?.let { photo ->
                                uploadPhoto(photo)
                            } ?: run {
                                Log.i(TAG, "No photo selected, use default")
                                val default_photo_name = "default_image.png"
                                val ref = FirebaseStorage.getInstance().getReference("/images/user_profile_image/$default_photo_name")
                                ref.downloadUrl.addOnSuccessListener {
                                    selectedPhoto = it
                                    saveUserIntoDatabase()
                                }
                            }
                        } else {
                            btnSignup.isClickable = true
                            Log.i(TAG, "Create user failed")
                        }
                    }.addOnFailureListener {
                        progressBar.visibility = View.INVISIBLE
                        btnSignup.isClickable = true
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                    }

            }
        }
    }

    private fun saveUserIntoDatabase() {
        currentUser?.let { user ->

            val uid = user.uid
            Log.i(TAG, uid)
            val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

            val uploadUser = User(listOf(), user.email!!, "", "", "", "", selectedPhoto.toString(), user.uid)
            Log.i(TAG, "user upload created")
            ref.setValue(uploadUser)
                .addOnSuccessListener {
                    getApp().currentUser = currentUser

                    val intent = Intent(this, UserProfileActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)

                }.addOnFailureListener {
                    progressBar.visibility = View.GONE

                    Log.i(TAG, "user upload failed")
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode== 0 && resultCode == Activity.RESULT_OK && data != null) {
            Log.i(TAG, "photo selected")

            selectedPhoto = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhoto)

            val bitmapDrawable = BitmapDrawable(bitmap)
            logo.setBackgroundDrawable(bitmapDrawable)
        } else {
            Toast.makeText(this, "Failed to select photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadPhoto(photoUri: Uri) {
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/user_profile_image/$filename")
        ref.putFile(photoUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    Log.i(TAG, "Photo uploaded, uri = $it")
                    selectedPhoto = it
                    saveUserIntoDatabase()
                }
            }
    }

    companion object {
        const val TAG = "sean"
    }
}

