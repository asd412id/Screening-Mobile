package com.asd412id.screening.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.asd412id.screening.R
import com.asd412id.screening.ScreenActivity
import com.asd412id.screening.models.Kegiatan

class KegiatanAdapter(private val kegiatan: MutableList<Kegiatan>): RecyclerView.Adapter<KegiatanAdapter.KegiatanHolder>() {

    lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KegiatanHolder {
        return KegiatanHolder(LayoutInflater.from(parent.context).inflate(R.layout.kegiatan_list,parent,false))
    }

    override fun onBindViewHolder(holder: KegiatanHolder, position: Int) {
        holder.bindKegiatan(kegiatan[position])
    }

    override fun getItemCount(): Int = kegiatan.size

    inner class KegiatanHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val uuid = view.findViewById<TextView>(R.id.uuid)
        private val name = view.findViewById<TextView>(R.id.name)
        private val tanggal = view.findViewById<TextView>(R.id.tanggal)
        private val kegiatanCard = view.findViewById<CardView>(R.id.kegiatan_card)
        private val loading = view.findViewById<ProgressBar>(R.id.progressBar)
        private val context = view.context

        fun bindKegiatan(kegiatan: Kegiatan) {
            if (kegiatan.loading){
                loading.visibility = View.VISIBLE
                kegiatanCard.visibility = View.GONE
            }else{
                loading.visibility = View.GONE
                kegiatanCard.visibility = View.VISIBLE
                if (kegiatan.uuid != ""){
                    uuid.text = kegiatan.uuid
                    name.text = kegiatan.name
                    tanggal.text = kegiatan.tanggal

                    if (kegiatan.no.mod(2) == 0){
                        kegiatanCard.setCardBackgroundColor(Color.parseColor("#efefef"))
                    }else{
                        kegiatanCard.setCardBackgroundColor(Color.parseColor("#ffffff"))
                    }

                    kegiatanCard.setOnClickListener {
                        val i = Intent(context, ScreenActivity::class.java)
                        i.putExtra("uuid",kegiatan.uuid)
                        i.putExtra("name",kegiatan.name)
                        context.startActivity(i)
                    }
                }else{
                    uuid.visibility = View.GONE
                    name.text = kegiatan.name
                    tanggal.visibility = View.GONE
                    name.setPadding(30,30,30,30)
                    name.textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
            }
        }
    }
}