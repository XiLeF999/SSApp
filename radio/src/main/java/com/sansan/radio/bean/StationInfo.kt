package com.sansan.radio.bean

/**
 * 电台信息数据类
 */
data class StationInfo(
    val id: Int,
    val frequency: Float,       // MHz
    val signalStrength: Int,    // dBm
    val isStereo: Boolean,      //立体声、单声道
    val name: String = "",      // 可从RDS获取
    val isFavorite: Boolean = false
)