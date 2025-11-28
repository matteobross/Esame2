package it.progmob.esame2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FileAdapter(
    private val files: List<File>,
    private val onPlay: (File) -> Unit,
    private val onUpload: (File) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtFileName)
        val btnUpload: ImageView = view.findViewById(R.id.btnUpload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.txtName.text = file.name

        holder.itemView.setOnClickListener { onPlay(file) }
        holder.btnUpload.setOnClickListener { onUpload(file) }
    }

    override fun getItemCount(): Int = files.size
}
