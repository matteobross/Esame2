package it.progmob.esame2

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.progmob.esame2.network.FileUploader
import kotlinx.coroutines.launch
import java.io.File

class PlayActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var txtTitle: TextView
    private lateinit var btnStop: Button
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        txtTitle = findViewById(R.id.txtTitle)
        btnStop = findViewById(R.id.btnStop)
        recyclerView = findViewById(R.id.rvFiles)

        recyclerView.layoutManager = LinearLayoutManager(this)
        loadFiles()

        btnStop.setOnClickListener { stopPlayback() }
    }

    private fun loadFiles() {
        val dir = externalCacheDir
        val files = dir?.listFiles { f ->
            f.name.endsWith(".m4a") || f.name.endsWith(".mp3")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        if (files.isEmpty()) {
            Toast.makeText(this, "Nessun file trovato", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = FileAdapter(
            files,
            onPlay = { file ->
                playRecording(file)
            },
            onUpload = { file ->
                lifecycleScope.launch {
                    val ok = FileUploader.upload(file.absolutePath)
                    Toast.makeText(
                        this@PlayActivity,
                        if (ok) "✔ Inviato al server" else "❌ Errore invio",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        recyclerView.adapter = adapter
    }

    private fun playRecording(file: File) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                Toast.makeText(this@PlayActivity, "▶️ ${file.name}", Toast.LENGTH_SHORT).show()
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            stop()
            release()
        }
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

