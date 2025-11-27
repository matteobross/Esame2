package it.progmob.esame2

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import androidx.lifecycle.lifecycleScope
import it.progmob.esame2.network.FileUploader
import kotlinx.coroutines.launch

class PlayActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var listView: ListView
    private lateinit var btnStop: Button
    private lateinit var txtTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        txtTitle = findViewById(R.id.txtTitle)
        btnStop = findViewById(R.id.btnStop)
        listView = findViewById(R.id.listViewFiles)

        txtTitle.text = "📁 Seleziona una registrazione"

        // Carica lista file audio
        loadFileList()

        // Click su un file = riproduci
        listView.setOnItemClickListener { _, _, position, _ ->
            val name = listView.adapter.getItem(position) as String
            val path = "${externalCacheDir?.absolutePath}/$name"
            playRecording(path)
        }

        btnStop.setOnClickListener { stopPlayback() }
    }

    private fun loadFileList() {

        val files = externalCacheDir?.listFiles { file ->
            val name = file.name.lowercase()
            name.endsWith(".m4a") ||
                    name.endsWith(".mp3") ||
                    name.endsWith(".wav") ||
                    name.endsWith(".aac") ||
                    name.endsWith(".ogg") ||
                    name.endsWith(".flac") ||
                    name.endsWith(".3gp")
        }?.sortedByDescending { it.lastModified() }

        if (files.isNullOrEmpty()) {
            Toast.makeText(this, "Nessun file trovato", Toast.LENGTH_SHORT).show()
            return
        }

        val names = files.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        listView.adapter = adapter
    }

    private fun playRecording(path: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                Toast.makeText(this@PlayActivity, "▶️ Riproduzione: $path", Toast.LENGTH_SHORT).show()
                setOnCompletionListener {
                    Toast.makeText(this@PlayActivity, "✔ Fine", Toast.LENGTH_SHORT).show()
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

