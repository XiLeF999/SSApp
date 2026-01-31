package com.sansan.radio.manager

object SSRadioManager {

    var onRadioControlListener: OnRadioControlListener? = null
    var onRadioChangeListener: OnRadioChangeListener? = null


    fun autoScan() {
        onRadioControlListener?.autoScan()
    }

    fun stopScan() {
        onRadioControlListener?.stopScan()
    }

    fun playStation(frequency: Float) {
        onRadioControlListener?.playStation(frequency)
    }

    fun setAutoScan(stations: ShortArray) {
        onRadioChangeListener?.setAutoScan(stations)
    }

    fun setStopScan(stop: Boolean) {
        onRadioChangeListener?.setStopScan(stop)
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
        fun stopScan()
        fun playStation(frequency: Float)
    }

    /**
     * Activity被动改变接口
     */
    interface OnRadioChangeListener {
        /**
         * 扫描结果，通知Activity
         */
        fun setAutoScan(stations: ShortArray)
        /**
         * 停止是否成功，通知Activity
         */
        fun setStopScan(stop: Boolean)
    }

}