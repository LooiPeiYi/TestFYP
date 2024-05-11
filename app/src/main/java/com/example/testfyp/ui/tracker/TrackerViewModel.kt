package com.example.testfyp.ui.tracker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TrackerViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is tracker Fragment"
    }
    val text: LiveData<String> = _text
}