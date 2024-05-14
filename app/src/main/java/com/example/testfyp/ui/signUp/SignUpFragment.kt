package com.example.testfyp.ui.signUp

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.navigation.fragment.findNavController
import com.example.testfyp.R
import com.example.testfyp.dataModel.user
import com.example.testfyp.ui.signUp.SignUpViewModel

class SignUpFragment : Fragment() {

    private lateinit var signUpViewModel: SignUpViewModel
    private var profilePicUri: Uri? = null
    private lateinit var selectedImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_signup, container, false)

        signUpViewModel = ViewModelProvider(this).get(SignUpViewModel::class.java)

        val usernameEditText: EditText = root.findViewById(R.id.Username)
        val emailEditText: EditText = root.findViewById(R.id.Email)
        val phoneEditText: EditText = root.findViewById(R.id.Phone)
        val passwordEditText: EditText = root.findViewById(R.id.Password)
        val confirmPasswordEditText: EditText = root.findViewById(R.id.CPassword)
        val signUpButton: Button = root.findViewById(R.id.signUpButton)
        val selectImageButton: Button = root.findViewById(R.id.imageSelect)
        selectedImageView = root.findViewById(R.id.selectedImageView)

        selectImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
        signUpButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim().toIntOrNull() ?: 0
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (password == confirmPassword) {
                val user = user(username, email, phone, password)
                signUpViewModel.saveData(user, requireContext(), profilePicUri) { imageUrl ->
                    if (imageUrl != null) {
                        val action = SignUpFragmentDirections.actionSignUpFragmentToProfileFragment(
                            username = username,
                            email = email,
                            phone = phone,
                            imageUrl = imageUrl
                        )
                        findNavController().navigate(action)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to upload image",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(context, "Password Mismatch", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            profilePicUri = data.data
            selectedImageView.setImageURI(profilePicUri)
        }
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
}
