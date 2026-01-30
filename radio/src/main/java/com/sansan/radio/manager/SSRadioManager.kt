package com.sansan.radio.manager

object SSRadioManager {

    var onRadioControlListener: OnRadioControlListener? = null
    var onRadioChangeListener: OnRadioChangeListener? = null


    fun autoScan() {
        onRadioControlListener?.autoScan()
    }



    fun addOnRadioControlListener(listener: OnRadioControlListener) {
        this.onRadioControlListener = listener
    }

    fun addOnRadioChangeListener(listener: OnRadioChangeListener) {
        this.onRadioChangeListener = listener
    }

    fun removeOnRadioControlListener() {
        this.onRadioControlListener = null
    }

    fun removeOnRadioChangeListener() {
        this.onRadioChangeListener = null
    }

    /**
     * Activity主动控制接口
     */
    interface OnRadioControlListener {
        fun autoScan()
    }

    /**
     * Activity被动改变接口
     */
    interface OnRadioChangeListener {
        fun setUsbState(state: Int)
    }

}