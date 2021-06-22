package com.asd412id.screening.modules

class UserApi {
    fun getData(): String {
        return ApiConnection().baseURL()+"/user"
    }
    fun login(): String {
        return ApiConnection().baseURL()+"/login"
    }
    fun logout(): String {
        return ApiConnection().baseURL()+"/logout"
    }
}