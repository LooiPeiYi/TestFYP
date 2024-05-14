package com.example.testfyp.ui.signUp

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.example.testfyp.dataModel.user
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class SignUpViewModel : ViewModel() {
    fun saveData(
        user: user,
        context: Context,
        profilePicUri: Uri?,
        onComplete: (String?) -> Unit
    ) {
        val firestoreRef = Firebase.firestore.collection("user").document(user.userName)

        firestoreRef.set(user)
            .addOnSuccessListener {
                Toast.makeText(context, "Saved user successfully", Toast.LENGTH_SHORT).show()

                profilePicUri?.let { uri ->
                    uploadImageToFirestore(user.userName, uri) { imageUrl ->
                        if (imageUrl != null) {
                            val data = mapOf("imageUrl" to imageUrl)
                            firestoreRef.set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    onComplete(imageUrl)
                                }
                                .addOnFailureListener {
                                    onComplete(null)
                                }
                        } else {
                            onComplete(null)
                        }
                    }
                } ?: run {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                onComplete(null)
            }
    }

    fun checkPassword(
        username: String,
        password: String,
        context: Context,
        data: (Result<Boolean>) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        // Check if the username exists
        checkUsernameExists(username, context) { usernameResult ->
            usernameResult.onSuccess { isUsernameExists ->
                if (isUsernameExists) {
                    // Username exists, now check the password
                    val firestoreRef = Firebase.firestore.collection("user")
                    try {
                        firestoreRef.whereEqualTo("userName", username).get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    for (document in task.result) {
                                        val user = document.toObject(user::class.java)
                                        if (checkPassword2(password, user.password)) {
                                            // Password correct, return true
                                            data(Result.success(true))
                                            return@addOnCompleteListener
                                        }
                                    }
                                    // No user found with the correct password
                                    data(Result.success(false))
                                } else {
                                    // Handle other exceptions
                                    data(
                                        Result.failure(
                                            task.exception ?: Exception("Unknown error")
                                        )
                                    )
                                }
                            }
                    } catch (e: Exception) {
                        // Handle exceptions
                        data(Result.failure(e))
                    }
                } else {
                    // Username not found
                    data(Result.success(false))
                }
            }.onFailure { exception ->
                // Handle failure to check username
                data(Result.failure(exception))
            }
        }
    }

    fun checkUsernameExists(
        username: String,
        context: Context,
        data: (Result<Boolean>) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {

        val firestoreRef = Firebase.firestore
            .collection("user")

        try {
            firestoreRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val isUsernameExists = !task.result.isEmpty
                    data(Result.success(isUsernameExists))
                } else {
                    // Handle other exceptions
                    data(Result.failure(task.exception ?: Exception("Unknown error")))
                }
            }
        } catch (e: Exception) {
            // Handle exceptions
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            data(Result.failure(e))
        }
    }

    fun checkPassword2(inputPassword: String, storedPassword: String): Boolean {
        return inputPassword == storedPassword
    }

    fun deleteData(
        userName: String,
        context: Context,
        navController: NavController
    ) = CoroutineScope(Dispatchers.IO).launch {

        val firestoreRef = Firebase.firestore
            .collection("user")
            .document(userName)

        try {
            firestoreRef.delete()
                .addOnSuccessListener {
                    Toast.makeText(context, "Successfully Delete Data", Toast.LENGTH_SHORT).show()
                    navController.navigate("SignUp")
                }
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    fun updateData(
        username: String,
        fieldsToUpdate: Map<String, Any>,
        context: Context,
        newProfilePicUri: Uri?, // URI of the new profile picture
        data: (Result<Unit>) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {

        // First, check if a new username is provided
        val newUsername = fieldsToUpdate["userName"] as? String ?: username
        val oldUsername = username

        val usernameUpdated = newUsername != null && newUsername != oldUsername

        // If username is being updated, delete the old document
        if (usernameUpdated) {
            val oldDocRef = Firebase.firestore
                .collection("user")
                .document(oldUsername)
            oldDocRef.delete()
                .addOnSuccessListener {
                    // Once the old document is deleted, proceed with updating the new document
                    newProfilePicUri?.let { uri ->
                        uploadImageToFirestore(newUsername, uri) { imageUrl ->
                            // Once the new image is uploaded, update the Firestore document with the new image URL
                            val firestoreRef = Firebase.firestore
                                .collection("user")
                                .document(newUsername)

                            // Add imageUrl to fieldsToUpdate map
                            val updatedFields = fieldsToUpdate.toMutableMap()
                            imageUrl?.let { url ->
                                updatedFields["imageUrl"] = url
                            }

                            // Update fields in Firestore
                            firestoreRef.set(updatedFields, SetOptions.merge())
                                .addOnSuccessListener {
                                    // Handle success if needed
                                    data(Result.success(Unit))
                                }
                                .addOnFailureListener { e ->
                                    // Handle failure if needed
                                    Toast.makeText(
                                        context,
                                        "Failed to update fields: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    data(Result.failure(e))
                                }
                        }
                    } ?: run {
                        // If no new profile picture URI is provided, just update the other fields in Firestore
                        val firestoreRef = Firebase.firestore
                            .collection("user")
                            .document(newUsername)

                        // Update fields in Firestore
                        firestoreRef.set(fieldsToUpdate, SetOptions.merge())
                            .addOnSuccessListener {
                                // Handle success if needed
                                data(Result.success(Unit))
                            }
                            .addOnFailureListener { e ->
                                // Handle failure if needed
                                Toast.makeText(
                                    context,
                                    "Failed to update fields: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                data(Result.failure(e))
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Handle failure if needed
                    Toast.makeText(
                        context,
                        "Failed to delete old document: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    data(Result.failure(e))
                }
        } else {
            // If username is not being updated, proceed with updating the document
            newProfilePicUri?.let { uri ->
                uploadImageToFirestore(oldUsername, uri) { imageUrl ->
                    // Once the new image is uploaded, update the Firestore document with the new image URL
                    val firestoreRef = Firebase.firestore
                        .collection("user")
                        .document(oldUsername)

                    // Add imageUrl to fieldsToUpdate map
                    val updatedFields = fieldsToUpdate.toMutableMap()
                    imageUrl?.let { url ->
                        updatedFields["imageUrl"] = url
                    }

                    // Update fields in Firestore
                    firestoreRef.set(updatedFields, SetOptions.merge())
                        .addOnSuccessListener {
                            // Handle success if needed
                            data(Result.success(Unit))
                        }
                        .addOnFailureListener { e ->
                            // Handle failure if needed
                            Toast.makeText(
                                context,
                                "Failed to update fields: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            data(Result.failure(e))
                        }
                }
            } ?: run {
                // If no new profile picture URI is provided, just update the other fields in Firestore
                val firestoreRef = Firebase.firestore
                    .collection("user")
                    .document(oldUsername)

                // Update fields in Firestore
                firestoreRef.set(fieldsToUpdate, SetOptions.merge())
                    .addOnSuccessListener {
                        // Handle success if needed
                        data(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        // Handle failure if needed
                        Toast.makeText(
                            context,
                            "Failed to update fields: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        data(Result.failure(e))
                    }
            }
        }
    }


    private fun uploadImageToFirestore(username: String, uri: Uri, onComplete: (String?) -> Unit) {
        val storageRef = Firebase.storage.reference.child("profilePictures/$username")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onComplete(downloadUri.toString())
                }.addOnFailureListener {
                    onComplete(null)
                }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }
}