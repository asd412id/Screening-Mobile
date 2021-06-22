package com.asd412id.screening.modules

class ApiConnection {
    fun baseURL(): String {
        val api = "api/v1"
        return "https://scr.smpn39sinjai.sch.id/$api"
//        return "http://192.168.43.13:3200/$api"
    }
}