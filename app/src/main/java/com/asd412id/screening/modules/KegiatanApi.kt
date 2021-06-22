package com.asd412id.screening.modules

class KegiatanApi {
    fun list(): String {
        return ApiConnection().baseURL()+ "/kegiatan"
    }
    fun peserta(): String {
        return ApiConnection().baseURL()+ "/peserta"
    }
    fun screen(): String {
        return ApiConnection().baseURL()+ "/screen"
    }
    fun search(): String {
        return ApiConnection().baseURL()+ "/cari"
    }
}