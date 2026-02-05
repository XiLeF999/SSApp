package com.sansan.radio.bean

/**
 * 电台信息数据类
 */
data class StationInfo(
    val station: Int,
    val name: String,
    val rds: String,
    var isFavorite: Int,
    var isSearch: Int,
    var isSelect: Boolean
)