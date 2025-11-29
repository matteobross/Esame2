package it.progmob.esame2

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

class PlayActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var txtTitle: TextView
    private lateinit var btnStop: Button
    private lateinit var recyclerView: RecyclerView

    private val client = OkHttpClient()

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
                uploadAudio(file)
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

    /**
     *  UPLOAD DEL FILE AL SERVER FASTAPI
     */
    private fun uploadAudio(file: File) {

        Toast.makeText(this, "⏳ Invio al server...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.IO) {

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody("audio/m4a".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("http://84.8.250.185:8000/analyze")
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@PlayActivity,
                            "❌ Errore upload: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val json = response.body?.string()

                    runOnUiThread {
                        if (json == null) {
                            Toast.makeText(
                                this@PlayActivity,
                                "❌ Risposta vuota dal server",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@runOnUiThread
                        }

                        Toast.makeText(
                            this@PlayActivity,
                            "✔ Analisi completata",
                            Toast.LENGTH_SHORT
                        ).show()

                        // PASSA I RISULTATI ALLA ARRANGEMENT ACTIVITY
                        val intent = Intent(this@PlayActivity, ArrangerActivity::class.java)
                        intent.putExtra("analysis_json", json)
                        startActivity(intent)
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
