package com.example.testfyp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.testfyp.R

class ProfileFragment : Fragment(R.layout.fragment_profile) {
    private val args: ProfileFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val username = args.username
        val email = args.email
        val phone = args.phone
        val imageUrl = args.imageUrl

        view.findViewById<TextView>(R.id.usernameTextView).text = username
        view.findViewById<TextView>(R.id.emailTextView).text = email
        view.findViewById<TextView>(R.id.phoneTextView).text = phone.toString()

        val imageView = view.findViewById<ImageView>(R.id.profileImageView)
        imageView.load(imageUrl) {
            placeholder(R.drawable.loading) // Optional: a placeholder image
            error(R.drawable.error) // Optional: an error image
        }
    }
}
