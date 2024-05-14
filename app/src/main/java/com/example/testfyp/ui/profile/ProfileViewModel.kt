package com.example.testfyp.ui.profile

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testfyp.dataModel.user
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    fun retrieveData(
        username: String,
        context: Context,
        data: (Result<user>) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        val firestoreRef = Firebase.firestore
            .collection("user")
            .document(username)


        try {
            firestoreRef.get()
                .addOnSuccessListener {
                    if (it.exists()) {
                        val user = it.toObject<user>()!!
                        data(Result.success(user))
                    } else {
                        Toast.makeText(context, "No Data Found", Toast.LENGTH_SHORT).show()

                    }
                }
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }

    }
    fun loadUserData(username: String, context: Context, onResult: (Result<user>) -> Unit) {
        viewModelScope.launch {
            retrieveData(username, context, onResult)
        }
    }
}