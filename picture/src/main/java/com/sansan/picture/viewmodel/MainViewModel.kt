package com.sansan.picture.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count
    fun increase() {
        _count.value += 1
    }
    fun decrease() {
        _count.value -= 1
    }


}