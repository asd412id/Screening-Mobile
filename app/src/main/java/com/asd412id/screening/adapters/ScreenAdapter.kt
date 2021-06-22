package com.asd412id.screening.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.asd412id.screening.R
import com.asd412id.screening.models.Screen

class ScreenAdapter(private val screen: MutableList<Screen>, private val listener: View.OnClickListener,private val longclick: View.OnLongClickListener): RecyclerView.Adapter<ScreenHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenHolder {
        return ScreenHolder(LayoutInflater.from(parent.context).inflate(R.layout.screen_list,parent,false))
    }

    override fun onBindViewHolder(holder: ScreenHolder, position: Int) {
        holder.bindScreen(screen[position], listener, longclick)
    }

    override fun getItemCount(): Int = screen.size
}

class ScreenHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val id = view.findViewById<TextView>(R.id.dataid)
    private val pid = view.findViewById<TextView>(R.id.pid)
    private val uuid = view.findViewById<TextView>(R.id.uuid)
    private val credential = view.findViewById<TextView>(R.id.credential)
    private val name = view.findViewById<TextView>(R.id.name)
    private val role = view.findViewById<TextView>(R.id.role)
    private val status = view.findViewById<TextView>(R.id.status)
    private val screenCard = view.findViewById<CardView>(R.id.screen_card)
    private val loading = view.findViewById<ProgressBar>(R.id.progressBar)

    @SuppressLint("SetTextI18n")
    fun bindScreen(
        screen: Screen,
        listener: View.OnClickListener,
        longclick: View.OnLongClickListener
    ) {
        if (screen.loading){
            loading.visibility = View.VISIBLE
            screenCard.visibility = View.GONE
        }else{
            loading.visibility = View.GONE
            screenCard.visibility = View.VISIBLE
            if (screen.id != ""){
                id.text = screen.id
                pid.text = screen.pid
                uuid.text = screen.uuid
                name.text = screen.name
                role.text = screen.role
                credential.text = screen.credential
                status.text = screen.status.toString()

                if (screen.status==0)
                    screenCard.setCardBackgroundColor(Color.parseColor("#dfffdf"))
                else
                    screenCard.setCardBackgroundColor(Color.parseColor("#ffe6e6"))

                screenCard.setOnClickListener(listener)
                screenCard.setOnLongClickListener(longclick)
            }else{
                id.visibility = View.GONE
                pid.visibility = View.GONE
                uuid.visibility = View.GONE
                name.text = screen.name
                role.visibility = View.GONE
                credential.visibility = View.GONE
                status.visibility = View.GONE
                name.setPadding(30,30,30,30)
                name.textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
        }
    }
}