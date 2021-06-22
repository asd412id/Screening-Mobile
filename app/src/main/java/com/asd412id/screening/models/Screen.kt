package com.asd412id.screening.models

data class Screen(
    val id: String = "",
    val uuid: String = "",
    val pid: String = "",
    val kuuid: String = "",
    val name: String = "",
    val credential: String = "",
    val role: String = "",
    val status: Int = 0,
    val prokes: Boolean = true,
    val suhu: String = "",
    val kondisi: String = "",
    val keterangan: String = "",
    val loading: Boolean = false
)