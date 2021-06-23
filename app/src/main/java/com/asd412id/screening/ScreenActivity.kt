package com.asd412id.screening

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.asd412id.screening.adapters.ScreenAdapter
import com.asd412id.screening.models.Screen
import com.asd412id.screening.modules.KegiatanApi
import com.asd412id.screening.modules.Storages
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

@Suppress("NAME_SHADOWING")
class ScreenActivity : AppCompatActivity() {
    private lateinit var b: Bundle
    lateinit var uuid: String
    lateinit var builder: AlertDialog.Builder
    lateinit var dialog: AlertDialog
    private lateinit var scscreen: RecyclerView
    lateinit var refresh: SwipeRefreshLayout
    lateinit var token: JSONObject
    private lateinit var screenAdapter: ScreenAdapter
    private lateinit var listScreen: MutableList<Screen>
    private lateinit var fab: FloatingActionButton
    private lateinit var fpid: TextView
    private lateinit var fname: TextView
    private lateinit var fstatus: Spinner
    private lateinit var fprokes: Spinner
    private lateinit var fkondisi: EditText
    private lateinit var fsuhu: EditText
    private lateinit var fketerangan: EditText
    private lateinit var fsimpan: Button
    private lateinit var fcancel: Button
    private lateinit var handler: Handler
    private var isSearch: String? = null
    private lateinit var lm: LinearLayoutManager
    private var total: Int? = 0
    private var count: Int? = 0
    private var page: Int? = 0
    private var vpage: Int? = 0
    private var isLoading: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen)
        b = intent.extras!!
        uuid = b.getString("uuid").toString()
        lm = LinearLayoutManager(this)

        supportActionBar!!.title = b.getString("name")
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        handler = Handler(Looper.getMainLooper())

        scscreen = findViewById(R.id.screen_recycle)
        refresh = findViewById(R.id.refresh)
        fab = findViewById(R.id.scan)
        builder = AlertDialog.Builder(this)

        token = Storages(this).getData("user","credential")!!
        loadScreen()

        refresh.setOnRefreshListener {
            if(isSearch == null) loadScreen() else queryPeserta(isSearch,"query")
        }

        fab.setOnClickListener {
            val i = Intent(this,ScanActivity::class.java)
            i.putExtra("uuid",uuid)
            i.putExtra("kname",b.getString("name"))
            startActivity(i)
        }

        scscreen.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0){
                    val vItem = lm.childCount
                    val lItem = lm.findFirstCompletelyVisibleItemPosition()
                    val count = screenAdapter.itemCount

                    if (!isLoading){
                        if (lItem + vItem >= count+1 && count < total!!){
                            if(isSearch == null){
                                isLoading = true
                                listScreen.let {
                                    listScreen.add(Screen(loading=true))
                                    screenAdapter.let { screenAdapter.notifyDataSetChanged() }
                                }
                                loadScreen(true)
                            }else{
                                if (isSearch != "" && isSearch != null){
                                    isLoading = true
                                    listScreen.let {
                                        listScreen.add(Screen(loading=true))
                                        screenAdapter.let { screenAdapter.notifyDataSetChanged() }
                                    }
                                    queryPeserta(isSearch,"query",true)
                                }
                            }
                        }
                    }
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
    }

    private fun loadScreen(loadMore: Boolean = false) {
        var url: String = KegiatanApi().screen()
        if (loadMore){
            vpage = page!!+1
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
            reloadAdapter(response, null)
        }, { error ->
            refresh.isRefreshing = false
            if (error?.networkResponse != null){
                val errorJSON = JSONObject(String(error.networkResponse.data))
                reloadAdapter(errorJSON,null)
            }else{
                showError("Tidak dapat terhubung ke server")
            }
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Authorization"] = "Bearer " + token.getString("api_token")
                headers["Kegiatan-Id"] = uuid
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

    private fun reloadAdapter(response: JSONObject, s: String?) {
        val jsonArray: JSONArray = response.getJSONArray("data")
        if (count!! == 0){
            listScreen = mutableListOf()
        }
        for (i in 0 until jsonArray.length()){
            val item = jsonArray.getJSONObject(i)
            listScreen.add(
                Screen(
                    id = item.getString("id"),
                    uuid = item.getString("uuid"),
                    pid = item.getString("pid"),
                    kuuid = item.getString("kuuid"),
                    name = item.getString("name"),
                    credential = item.getString("credential"),
                    role = item.getString("role"),
                    status = item.getInt("status"),
                    prokes = item.getBoolean("prokes"),
                    suhu = item.getString("suhu"),
                    kondisi = item.getString("kondisi"),
                    keterangan = item.getString("keterangan"),
                )
            )
        }

        if (count!! == 0){
            screenAdapter = ScreenAdapter(listScreen, {
                val jsonData = JSONObject()
                jsonData.put("kuuid",uuid)
                jsonData.put("credential",it.findViewById<TextView>(R.id.credential).text.toString())
                jsonData.put("status",it.findViewById<TextView>(R.id.status).text.toString())
                loadPeserta(jsonData,s)
            }, {
                if (s == null){
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Hapus data ini?")
                    val suuid: String = it.findViewById<TextView>(R.id.uuid).text.toString()
                    val name: String = it.findViewById<TextView>(R.id.name).text.toString()
                    val role: String = it.findViewById<TextView>(R.id.role).text.toString()

                    builder.setMessage("$name\n$role")
                    builder.setPositiveButton("Ya") { _, _ ->
                        hapusPeserta(suuid)
                    }
                    builder.setNegativeButton("Tidak") { dialog, _ ->
                        dialog.dismiss()
                    }
                    val dialog = builder.create()
                    dialog.show()
                }
                true
            })

            scscreen.apply {
                layoutManager = lm
                adapter = screenAdapter
            }
        }
        response.has("total").let { total = response.getInt("total") }
        response.has("count").let {
            if (isLoading && count!! > 0){
                listScreen.let {
                    listScreen.removeAt(count!!)
                }
            }
            count = count?.plus(response.getInt("count"))
        }
        response.has("page").let { page = response.getInt("page") }
        screenAdapter.notifyDataSetChanged()
        isLoading = false
    }

    private fun hapusPeserta(suuid: String) {
        builder.setMessage("Menghapus data ...")
        builder.setCancelable(false)
        dialog = builder.create()
        dialog.show()
        val queue = Volley.newRequestQueue(this)
        val request = object: JsonObjectRequest(Method.DELETE, KegiatanApi().peserta(), null, { response ->
            dialog.dismiss()
            builder.setMessage(response.getString("status"))
            dialog = builder.create()
            dialog.show()
            Handler(Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                reloadData()
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
                headers["Screen-Id"] = suuid
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

    private fun loadPeserta(screen: JSONObject, s: String?) {
        val form: View = layoutInflater.inflate(R.layout.screen_form,null)
        fpid = form.findViewById(R.id.pid)
        fname = form.findViewById(R.id.name)
        fstatus = form.findViewById(R.id.status)
        fprokes = form.findViewById(R.id.prokes)
        fkondisi = form.findViewById(R.id.kondisi)
        fsuhu = form.findViewById(R.id.suhu)
        fketerangan = form.findViewById(R.id.keterangan)
        fsimpan = form.findViewById(R.id.submit)
        fcancel = form.findViewById(R.id.cancel)

        val builder = AlertDialog.Builder(this)
        builder.setMessage("Memuat data ...")
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
        val queue = Volley.newRequestQueue(this)
        val request = object: JsonObjectRequest(Method.GET, KegiatanApi().peserta(), null, { response ->
            dialog.dismiss()
            val builder = AlertDialog.Builder(this)
            builder.setView(form)
            val dialog = builder.create()
            dialog.show()
            formLoad(response.getJSONObject("data"),dialog)
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
                headers["Kegiatan-Id"] = screen.getString("kuuid")
                headers["Credential"] = screen.getString("credential")
                if (s=="query"){
                    headers["Type"] = "query"
                }else{
                    headers["Status"] = screen.getString("status")
                }
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
        if (isLoading && count!! > 0){
            listScreen.let {
                listScreen.removeAt(count!!)
                screenAdapter.notifyDataSetChanged()
            }
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

    private fun formLoad(response: JSONObject, dialog: AlertDialog) {
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
            dialog.dismiss()
            val jsonSubmit = JSONObject()
            jsonSubmit.put("peserta_id",response.getString("id").toInt())
            jsonSubmit.put("role",response.getString("role"))
            jsonSubmit.put("jenis_screening",fstatus.selectedItemPosition)
            jsonSubmit.put("prokes",fprokes.selectedItemPosition)
            jsonSubmit.put("suhu",fsuhu.text)
            jsonSubmit.put("kondisi",fkondisi.text)
            jsonSubmit.put("keterangan",fketerangan.text)
            submitPeserta(jsonSubmit.toString())
        }
        fcancel.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun submitPeserta(data: String) {
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
                reloadData()
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
                headers["Kegiatan-Id"] = uuid
                headers["Credential"] = b.getString("qrcode").toString()
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

    private fun reloadData(){
        overridePendingTransition(0,0)
        finish()
        startActivity(intent)
        overridePendingTransition(0,0)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.screen_menu,menu)
        val searchItem: MenuItem? = menu?.findItem(R.id.action_search)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView: SearchView = searchItem?.actionView as SearchView
        searchView.queryHint = "Cari Peserta"
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                handler.removeCallbacksAndMessages(null)
                isSearch = ""
                if (newText != ""){
                    isSearch = newText
                    handler.postDelayed({
                        queryPeserta(newText,"query")
                    },500)
                }
                return false
            }

        })

        searchItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener{
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                reloadData()
                return true
            }
        })

        return true
    }

    private fun queryPeserta(query: String?, s: String? = null,loadMore: Boolean = false) {
        var url: String = KegiatanApi().search()
        if (loadMore){
            vpage = page!!+1
            url = "$url?page=$vpage"
        }else{
            refresh.isRefreshing = true
            total = 0
            count = 0
            page = 0
            vpage = 0
        }
        val queue = Volley.newRequestQueue(this)
        val request = object: JsonObjectRequest(Method.POST, url, null, { response ->
            refresh.isRefreshing = false
            reloadAdapter(response, s)
        }, { error ->
            refresh.isRefreshing = false
            if (error?.networkResponse != null){
                val errorJSON = JSONObject(String(error.networkResponse.data))
                reloadAdapter(errorJSON,null)
            }else{
                showError("Tidak dapat terhubung ke server")
            }
        }){
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Accept"] = "application/json"
                headers["Authorization"] = "Bearer " + token.getString("api_token")
                headers["Kegiatan-Id"] = uuid
                headers["Query"] = query.toString()
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

    override fun onBackPressed() {
        val i = Intent(this,KegiatanActivity::class.java)
        startActivity(i)
        finishAffinity()
    }
}