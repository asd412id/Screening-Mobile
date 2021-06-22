package com.asd412id.screening

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asd412id.screening.modules.Storages
import com.asd412id.screening.modules.UserApi
import kotlin.system.exitProcess

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            val token = Storages(this).getData("user","credential")
            if(token != null){
                val queue = Volley.newRequestQueue(this)
                val request = object: JsonObjectRequest(Method.GET, UserApi().getData(), null, {
                    startActivity(Intent(this,KegiatanActivity::class.java))
                    finishAffinity()
                }, {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.apply {
                        setTitle("Tidak dapat terhubung ke server")
                        setMessage("Pastikan perangkat terhubung ke internet!")
                        setPositiveButton("Hubungkan Kembali") { _, _ ->
                            overridePendingTransition(0, 0)
                            finish()
                            startActivity(intent)
                            overridePendingTransition(0, 0)
                        }
                        setNegativeButton("Tutup Aplikasi") { _, _ ->
                            moveTaskToBack(true)
                            exitProcess(-1)
                        }
                    }
                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }){
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["Accept"] = "application/json"
                        headers["Authorization"] = "Bearer " + token.getString("api_token")
                        return headers
                    }
                }
                request.retryPolicy = DefaultRetryPolicy(
                    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                    3,
                    1f
                )
                queue.add(request)
            }else{
                startActivity(Intent(this,Login::class.java))
                finishAffinity()
            }
        },1000)
    }
}