package com.sansan.radio.activity

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.sansan.radio.databinding.ActivityMainBinding

class MainActivity : FragmentActivity(){

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }


}