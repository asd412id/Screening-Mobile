package com.asd412id.screening

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asd412id.screening.modules.Storages
import com.asd412id.screening.modules.UserApi
import org.json.JSONObject

class Login : AppCompatActivity() {
    lateinit var loading: AlertDialog.Builder
    lateinit var dialog: AlertDialog
    lateinit var view: View
    lateinit var progresstext: TextView
    lateinit var progressbar: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val username: EditText = findViewById(R.id.username)
        val password: EditText = findViewById(R.id.password)
        val btnlogin: Button = findViewById(R.id.btnlogin)

        username.requestFocus()
        loading = AlertDialog.Builder(this)
        view = layoutInflater.inflate(R.layout.loading,null)
        progresstext = view.findViewById(R.id.progresstext)
        progressbar = view.findViewById(R.id.progressBar)
        loading.setView(view)
        loading.setCancelable(false)
        dialog = loading.create()

        btnlogin.setOnClickListener {
            progressbar.visibility = View.VISIBLE
            progresstext.text = "Silahkan tunggu ..."
            progresstext.textSize = 21f
            dialog.show()
            val queue = Volley.newRequestQueue(this)
            val request = object: JsonObjectRequest(Method.POST, UserApi().login(), null, { response ->
                dialog.dismiss()
                Storages(this).setData("user","credential",response)
                startActivity(Intent(this,KegiatanActivity::class.java))
                finishAffinity()
            }, { error ->
                if (error?.networkResponse != null){
                    val errorJSON = JSONObject(String(error.networkResponse.data))
                    if (error.networkResponse.statusCode == 406){
                        val err: String? = errorJSON.getString("error")
                        progresstext.text = err.toString()
                        progresstext.textSize = 17f
                        progressbar.visibility = View.GONE
                        Handler(Looper.getMainLooper()).postDelayed({
                            dialog.dismiss()
                        },2000)
                    }
                }else{
                    progresstext.text = "Tidak dapat terhubung ke server"
                    progresstext.textSize = 17f
                    progressbar.visibility = View.GONE
                    Handler(Looper.getMainLooper()).postDelayed({
                        dialog.dismiss()
                    },2000)
                }
            }){
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Accept"] = "application/json"
                    headers["Username"] = username.text.toString()
                    headers["Password"] = password.text.toString()
                    return headers
                }
            }
            request.retryPolicy = DefaultRetryPolicy(
                5000,
                1,
                1f
            )
            queue.add(request)
        }
    }
}