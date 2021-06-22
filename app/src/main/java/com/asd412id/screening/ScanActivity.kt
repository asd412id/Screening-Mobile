package com.asd412id.screening

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asd412id.screening.modules.KegiatanApi
import com.asd412id.screening.modules.Storages
import com.budiyev.android.codescanner.*
import org.json.JSONObject

class ScanActivity : AppCompatActivity() {
    lateinit var b: Bundle
    private lateinit var codeScanner: CodeScanner
    lateinit var scn: CodeScannerView
    lateinit var dialog: AlertDialog
    private lateinit var form: View
    private lateinit var token: JSONObject
    private lateinit var fpid: TextView
    private lateinit var fname: TextView
    private lateinit var fstatus: Spinner
    private lateinit var fprokes: Spinner
    private lateinit var fkondisi: EditText
    private lateinit var fsuhu: EditText
    private lateinit var fketerangan: EditText
    private lateinit var fsimpan: Button
    private lateinit var fcancel: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        supportActionBar!!.title = "Scan QR Code Peserta"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        b = intent.extras!!

        form = layoutInflater.inflate(R.layout.screen_form,null)
        token = Storages(this).getData("user","credential")!!

        fpid = form.findViewById(R.id.pid)
        fname = form.findViewById(R.id.name)
        fstatus = form.findViewById(R.id.status)
        fprokes = form.findViewById(R.id.prokes)
        fkondisi = form.findViewById(R.id.kondisi)
        fsuhu = form.findViewById(R.id.suhu)
        fketerangan = form.findViewById(R.id.keterangan)
        fsimpan = form.findViewById(R.id.submit)
        fcancel = form.findViewById(R.id.cancel)

        scn = findViewById(R.id.scn)
        codeScanner()
    }
    private fun codeScanner() {
        codeScanner = CodeScanner(this, scn)

        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS

            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false
            isTouchFocusEnabled = true

            decodeCallback = DecodeCallback {
                runOnUiThread {
                    loadPeserta(it.text)
                }
            }

        }
    }

    private fun loadPeserta(qrcode: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@ScanActivity)
        builder.setMessage("Mengecek data ...")
        builder.setCancelable(false)
        val dialog: AlertDialog = builder.create()
        dialog.show()
        codeScanner.stopPreview()
        val queue = Volley.newRequestQueue(this)
        val request = object: JsonObjectRequest(Method.GET, KegiatanApi().peserta(), null, { response ->
            dialog.dismiss()
            formLoad(response.getJSONObject("data"),qrcode)
        }, { error ->
            dialog.dismiss()
            if (error?.networkResponse != null){
                Log.i("ANU",String(error.networkResponse.data))
                val errorJSON = JSONObject(String(error.networkResponse.data))
                val err: String? = errorJSON.getString("error")
                if (err != null) {
                    showError(err)
                }
            }else{
                showError("Tidak dapat terhubung ke server")
            }
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Authorization"] = "Bearer " + token.getString("api_token")
                headers["Kegiatan-Id"] = b.getString("uuid").toString()
                headers["Credential"] = qrcode
                headers["Type"] = "query"
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

    private fun showError(error: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@ScanActivity)
        builder.setMessage(error)
        val dialog: AlertDialog = builder.create()
        dialog.show()
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            dialog.dismiss()
            overridePendingTransition(0,0)
            finish()
            startActivity(intent)
            overridePendingTransition(0,0)
        },1000)
    }

    private fun formLoad(response: JSONObject, qrcode: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@ScanActivity)
        builder.setView(form)
        val dialog: AlertDialog = builder.create()
        dialog.show()
        val jscreen = response.getJSONObject("screen")
        fpid.text = response.getString("pid")
        fname.text = response.getString("name")
        fkondisi.setText(if(jscreen.getString("kondisi") != "null") jscreen.getString("kondisi") else "")
        fsuhu.setText(if(jscreen.getString("suhu") != "0") jscreen.getString("suhu") else "")
        fketerangan.setText(if(jscreen.getString("keterangan") == "null") "" else jscreen.getString("keterangan"))

        val rstatus: List<String> = listOf("Datang","Pulang")
        val rprokes: List<String> = listOf("Tidak","Ya")

        val rstatusadapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,rstatus)
        val rprokesadapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,rprokes)

        fstatus.adapter = rstatusadapter
        fprokes.adapter = rprokesadapter

        fstatus.setSelection(jscreen.getInt("status"))
        fprokes.setSelection(jscreen.getInt("prokes"))

        fsuhu.requestFocus()
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        fsimpan.setOnClickListener {
            val jsonSubmit = JSONObject()
            jsonSubmit.put("peserta_id",response.getString("id"))
            jsonSubmit.put("role",response.getString("role"))
            jsonSubmit.put("jenis_screening",fstatus.selectedItemPosition)
            jsonSubmit.put("prokes",fprokes.selectedItemPosition)
            jsonSubmit.put("suhu",fsuhu.text)
            jsonSubmit.put("kondisi",fkondisi.text)
            jsonSubmit.put("keterangan",fketerangan.text)
            dialog.dismiss()
            submitPeserta(jsonSubmit.toString(), qrcode)
        }
        fcancel.setOnClickListener {
            dialog.dismiss()
            overridePendingTransition(0,0)
            finish()
            startActivity(intent)
            overridePendingTransition(0,0)
        }
    }

    private fun submitPeserta(data: String, qrcode: String) {
        token = Storages(this).getData("user","credential")!!
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Menyimpan data ...")
        builder.setCancelable(false)
        dialog = builder.create()
        dialog.show()
        val queue = Volley.newRequestQueue(this)
        val request = object: JsonObjectRequest(Method.PUT, KegiatanApi().peserta(), null, { response ->
            dialog.dismiss()
            builder.setMessage(response.getString("status"))
            dialog = builder.create()
            dialog.show()
            Handler(Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                val i = Intent(this,ScreenActivity::class.java)
                i.putExtra("uuid",b.getString("uuid").toString())
                i.putExtra("name",b.getString("kname").toString())
                startActivity(i)
                finishAffinity()
            },1000)
        }, { error ->
            dialog.dismiss()
            if (error?.networkResponse != null){
                val errorJSON = JSONObject(String(error.networkResponse.data))
                val err: String? = errorJSON.getString("error")
                if (err != null) {
                    showError(err)
                }
            }else{
                showError("Tidak dapat terhubung ke server")
            }
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Authorization"] = "Bearer " + token.getString("api_token")
                headers["Kegiatan-Id"] = b.getString("uuid").toString()
                headers["Credential"] = qrcode
                headers["Data-Submit"] = data
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

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
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
}