package it.progmob.esame2

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class NewRecActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private lateinit var outputFile: String

    private lateinit var btnRecord: Button
    private lateinit var btnAscolto: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newrec)

        btnRecord = findViewById(R.id.btnRecord)
        btnAscolto = findViewById(R.id.btnAscolto)



        // ---------CLICK BOTTONI ------
        btnRecord.setOnClickListener {

            // *** CONTROLLO PERMESSO ***
            if (!checkAudioPermission()) {
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 123)
                return@setOnClickListener
            }

            if (!isRecording) startRecording()
            else stopRecording()
        }

        btnAscolto.setOnClickListener {

            val intent = Intent(this, PlayActivity::class.java)

            startActivity(intent)
        }

    }
//--------------------------------------------------------------------------
//                 inizio funzioni
    //                PERMESSI MICROFONO
    private fun checkAudioPermission(): Boolean {
        val perm = android.Manifest.permission.RECORD_AUDIO
        return checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 123) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startRecording()
            } else {
                Toast.makeText(this, "Permesso microfono negato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // -----------------------------------------------------------
    //                REGISTRAZIONE AUDIO
    // -----------------------------------------------------------
    private fun startRecording() {
        outputFile = "${externalCacheDir?.absolutePath}/recording_${System.currentTimeMillis()}.m4a"
        try {
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()   // <-- aggiunto perchè se manca permesso crasha
                start()
            }

            isRecording = true
            btnRecord.text = "FERMA"
            Toast.makeText(this, "Registrazione avviata", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording() {
        //qua uso il metodo try per evitare che crashi, se uno di di questi metodi fallisce non crasha ma finisce la registrazione
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            isRecording = false
            btnRecord.text = "REGISTRA"

            Toast.makeText(this, "Registrazione salvata!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Errore nello stop: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
//da eliminare, vecchio modo

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            try {
                mediaRecorder?.stop()
            } catch (_: Exception) {}
        }
        mediaRecorder?.release()
    }
}
