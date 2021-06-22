package com.asd412id.screening.modules

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "UNCHECKED_CAST")
class Storages(private val context: Context) {
    private fun preferences(x: String): SharedPreferences {
        return context.getSharedPreferences(x,Context.MODE_PRIVATE)
    }

    fun getAllData(x: String): Map<String, JSONObject>? {
        val preferences: SharedPreferences? = preferences(x)
        if (preferences != null) {
            return preferences(x).all as Map<String, JSONObject>
        }
        return null
    }

    fun getData(x: String,key: String): JSONObject? {
        val preferences: SharedPreferences? = preferences(x)
        if (preferences != null && !preferences(x).getString(key,"").equals("")) {
            return JSONObject(preferences(x).getString(key,""))
        }
        return null
    }

    @SuppressLint("CommitPrefEdits")
    fun setData(x: String, key: String, data: JSONObject): JSONObject? {
        with(preferences(x).edit()){
            putString(key,data.toString())
            commit()
        }
        return getData(x,key)
    }

    fun destroyData(x: String, key: String) {
        with(preferences(x).edit()){
            remove(key)
            commit()
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun destroyAll(x: String){
        preferences(x).edit().clear().apply()
    }
}