package com.asd412id.screening

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asd412id.screening.adapters.KegiatanAdapter
import com.asd412id.screening.models.Kegiatan
import com.asd412id.screening.modules.KegiatanApi
import com.asd412id.screening.modules.Storages
import org.json.JSONArray
import org.json.JSONObject

class KegiatanActivity : AppCompatActivity() {
    private lateinit var dialog: AlertDialog
    private lateinit var cvkegiatan: RecyclerView
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var token: JSONObject
    private lateinit var kegiatanAdapter: KegiatanAdapter
    private var listKegiatan: MutableList<Kegiatan> = mutableListOf()
    private var isLoading:Boolean = false
    private var total = 0
    private var count = 0
    private var page = 0
    private var vpage = 0
    private var lm: LinearLayoutManager = LinearLayoutManager(this@KegiatanActivity)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupPermissions()
        setContentView(R.layout.activity_kegiatan)
        supportActionBar!!.title = "Daftar Kegiatan"
        cvkegiatan = findViewById(R.id.kegiatan_recycle)
        refresh = findViewById(R.id.refresh)

        token = Storages(this).getData("user","credential")!!
        loadKegiatan()
        refresh.setOnRefreshListener {
           loadKegiatan()
        }
        
        cvkegiatan.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0){
                    val vItem = lm.itemCount
                    val lItem = lm.findFirstCompletelyVisibleItemPosition()
                    val count = kegiatanAdapter.itemCount

                    if (!isLoading){
                        if (vItem + lItem >= count && count < total){
                            isLoading = true
                            listKegiatan.let {
                                listKegiatan.add(Kegiatan(loading=true))
                                kegiatanAdapter.notifyDataSetChanged()
                            }
                            loadKegiatan()
                        }
                    }
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
    }

    private fun loadKegiatan() {
        var url = KegiatanApi().list()
        if (isLoading){
            vpage = page.plus(1)
            url = "$url?page=$vpage"
        }else{
            refresh.isRefreshing = true
            total = 0
            count = 0
            page = 0
            vpage = 0
        }
        val queue = Volley.newRequestQueue(this)
        val request = object: JsonObjectRequest(Method.GET, url, null, { response ->
            refresh.isRefreshing = false
            reloadAdapter(response)
        }, { error ->
            refresh.isRefreshing = false
            if (error?.networkResponse != null){
                val errorJSON = JSONObject(String(error.networkResponse.data))
                reloadAdapter(errorJSON)
            }else{
                showError("Tidak dapat terhubung ke server")
            }
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Authorization"] = "Bearer " + token.getString("api_token")
                return headers
            }
        }
        request.retryPolicy = DefaultRetryPolicy(
            10000,
            1,
            1f
        )
        queue.add(request)
    }

    private fun reloadAdapter(response: JSONObject) {
        val jsonArray: JSONArray = response.getJSONArray("data")
        if (count == 0){
            listKegiatan = mutableListOf()
        }
        for (i in 0 until jsonArray.length()){
            val item = jsonArray.getJSONObject(i)
            val tanggal = item.getString("tanggal_slash")
            listKegiatan.add(
                Kegiatan(
                    uuid = item.getString("uuid"),
                    no = item.getInt("no"),
                    name = item.getString("name"),
                    tanggal = tanggal.toString()
                )
            )
        }
        if (count == 0){
            kegiatanAdapter = KegiatanAdapter(listKegiatan)
            cvkegiatan.apply {
                layoutManager = lm
                adapter = kegiatanAdapter
            }
        }
        if (isLoading && count > 0){
            listKegiatan.removeAt(count)
        }
        total = response.getInt("total")
        count = count.plus(response.getInt("count"))
        page = response.getInt("page")
        kegiatanAdapter.notifyDataSetChanged()
        isLoading = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> onBackPressed()
            R.id.logout -> {
                Storages(this).destroyData("user","credential")
                startActivity(Intent(this,Login::class.java))
                finishAffinity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showError(error: String){
        if (isLoading && count > 0){
            listKegiatan.removeAt(count)
            kegiatanAdapter.notifyDataSetChanged()
            isLoading = false
        }
        val builder = AlertDialog.Builder(this)
        builder.setMessage(error)
        val dialog = builder.create()
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        },1000)
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this, arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQ
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQ -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "You need the camera permission to use this app",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val CAMERA_REQ = 101
    }
}