package com.sansan.radio.bean

/**
 * 电台信息数据类
 */
data class StationInfo(
    val frequency: Int,
    val name: String,
    val rds: String,
    val isFavorite: Int
)