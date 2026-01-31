package com.sansan.radio.bean

/**
 * 电台信息数据类
 */
data class StationInfo(
    val id: Int,
    val frequency: Float,       // MHz
    val isFavorite: Boolean = false
)