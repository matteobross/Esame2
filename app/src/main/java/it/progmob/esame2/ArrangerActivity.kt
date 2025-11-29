package it.progmob.esame2

import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class ArrangerActivity : AppCompatActivity() {

    // UI
    private lateinit var txtKey: TextView
    private lateinit var txtBpm: TextView
    private lateinit var spinnerProgression: Spinner
    private lateinit var radioDrums: RadioGroup
    private lateinit var btnPlay: Button
    private lateinit var btnStop: Button

    // Audio
    private lateinit var soundPool: SoundPool
    // Player per la voce registrata
    private var voicePlayer: MediaPlayer? = null

    // Piano notes
    private val pianoNotes = HashMap<String, Int>()

    // Drum samples
    private var kickId = 0
    private var snareId = 0
    private var hihatId = 0

    // Loop engine
    private val handler = Handler(Looper.getMainLooper())
    private var isPlaying = false
    private var step = 0

    // VARIABILI PER SINCRONIZZAZIONE (Anti-Drift)
    private var startTime: Long = 0
    private var totalSixteenthsPlayed: Long = 0

    // VARIABILI MUSICALI
    private var bpm: Double = 120.0
    private var key = "C"
    private var duration: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arranger)

        txtKey = findViewById(R.id.txtKey)
        txtBpm = findViewById(R.id.txtBpm)
        spinnerProgression = findViewById(R.id.spinnerProgression)
        radioDrums = findViewById(R.id.radioDrums)
        btnPlay = findViewById(R.id.btnPlay)
        btnStop = findViewById(R.id.btnStop)

        // ------------------------------------------------------------------
        // 🔥 1. RICEVI E AGGIORNA I DATI DAL SERVER
        // ------------------------------------------------------------------
        val jsonString = intent.getStringExtra("analysis_json")

        if (jsonString != null) {
            try {
                val json = JSONObject(jsonString)
                bpm = json.optDouble("bpm", 120.0)
                key = json.optString("key", "C")
                duration = json.optDouble("duration", 0.0)
            } catch (e: Exception) {
                Toast.makeText(this, "Errore lettura JSON: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        txtKey.text = "Key: $key"
        txtBpm.text = String.format("BPM: %.1f", bpm)

        // ------------------------------------------------------------------
        // AGGIUNTA OPZIONE "Nessun Piano"
        // ------------------------------------------------------------------
        // Ora la lista include esplicitamente "Nessun Piano" all'inizio
        val progressions = listOf("Nessun Piano", "I - V - vi - IV", "ii - V - I", "vi - IV - I - V")

        spinnerProgression.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            progressions
        )
        // Selezioniamo la prima progressione vera (indice 1) di default
        spinnerProgression.setSelection(1)

        // Setup Audio
        soundPool = SoundPool.Builder().setMaxStreams(12).build()
        loadPianoNotes()
        loadDrumSamples()

        // ------------------------------------------------------------------
        // 🔥 2. CARICA LA VOCE IN LOOP
        // ------------------------------------------------------------------
        val voicePath = intent.getStringExtra("voice_path")
        if (voicePath != null) {
            voicePlayer = MediaPlayer().apply {
                setDataSource(voicePath)
                isLooping = true  // LOOP ATTIVO
                prepare()
            }
        }

        btnPlay.setOnClickListener { startAll() }
        btnStop.setOnClickListener { stopAll() }
    }

    // -------------------------------------------------------
    // START / STOP
    // -------------------------------------------------------

    private fun startAll() {
        if (isPlaying) return
        isPlaying = true

        step = 0
        totalSixteenthsPlayed = 0
        startTime = System.currentTimeMillis()

        voicePlayer?.start()
        handler.post(loopRunnable)
    }

    private fun stopAll() {
        isPlaying = false
        handler.removeCallbacks(loopRunnable)

        if (voicePlayer != null && voicePlayer!!.isPlaying) {
            voicePlayer?.pause()
        }
        voicePlayer?.seekTo(0)
    }

    // -------------------------------------------------------
    // ENGINE SINCRONIZZATO
    // -------------------------------------------------------

    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!isPlaying) return

            playDrums(step)
            playHarmony(step)

            step = (step + 1) % 16
            totalSixteenthsPlayed++

            val msPerBeat = 60000.0 / bpm
            val stepDuration = msPerBeat / 4.0

            val nextExpectedTime = startTime + (stepDuration * totalSixteenthsPlayed).toLong()
            val now = System.currentTimeMillis()

            var delay = nextExpectedTime - now
            if (delay < 0) delay = 0

            handler.postDelayed(this, delay)
        }
    }

    // -------------------------------------------------------
    // CARICAMENTO SUONI
    // -------------------------------------------------------

    private fun loadPianoNotes() {
        pianoNotes["C"]  = soundPool.load(this, R.raw.piano_c, 1)
        pianoNotes["C#"] = soundPool.load(this, R.raw.piano_csharp, 1)
        pianoNotes["D"]  = soundPool.load(this, R.raw.piano_d, 1)
        pianoNotes["D#"] = soundPool.load(this, R.raw.piano_dsharp, 1)
        pianoNotes["E"]  = soundPool.load(this, R.raw.piano_e, 1)
        pianoNotes["F"]  = soundPool.load(this, R.raw.piano_f, 1)
        pianoNotes["F#"] = soundPool.load(this, R.raw.piano_fsharp, 1)
        pianoNotes["G"]  = soundPool.load(this, R.raw.piano_g, 1)
        pianoNotes["G#"] = soundPool.load(this, R.raw.piano_gsharp, 1)
        pianoNotes["A"]  = soundPool.load(this, R.raw.piano_a, 1)
        pianoNotes["A#"] = soundPool.load(this, R.raw.piano_asharp, 1)
        pianoNotes["B"]  = soundPool.load(this, R.raw.piano_b, 1)
    }

    private fun loadDrumSamples() {
        kickId = soundPool.load(this, R.raw.kick, 1)
        snareId = soundPool.load(this, R.raw.snare, 1)
        hihatId = soundPool.load(this, R.raw.hihat, 1)
    }

    // -------------------------------------------------------
    // LOGICA MUSICALE
    // -------------------------------------------------------

    private fun playDrums(step: Int) {
        val style = when (radioDrums.checkedRadioButtonId) {
            R.id.radioPop -> "pop"
            R.id.radioRock -> "rock"
            R.id.radioReggae -> "reggae"
            R.id.radioNone -> "none" // Gestione Muto
            else -> "pop"
        }

        if (style == "none") return // Se è muto, esci subito

        when (style) {
            "pop" -> {
                // MODIFICATO: POP PIÙ LENTO (HiHat solo sugli ottavi)
                if (step == 0 || step == 8) soundPool.play(kickId,1f,1f,1,0,1f)
                if (step == 4 || step == 12) soundPool.play(snareId,1f,1f,1,0,1f)

                // HiHat suona solo sui passi pari (0, 2, 4...) invece che sempre
                if (step % 2 == 0) {
                    soundPool.play(hihatId, 0.4f,0.4f,1,0,1f)
                }
            }
            "rock" -> {
                if (step in listOf(0,2,8)) soundPool.play(kickId,1f,1f,1,0,1f)
                if (step == 4 || step == 12) soundPool.play(snareId,1f,1f,1,0,1f)
                if (step % 4 == 0) soundPool.play(hihatId,0.4f,0.4f,1,0,1f)
            }
            "reggae" -> {
                if (step == 8) {
                    soundPool.play(kickId,1f,1f,1,0,1f)
                    soundPool.play(snareId,1f,1f,1,0,1f)
                }
                if (step in listOf(2,6,10,14))
                    soundPool.play(hihatId,0.7f,0.7f,1,0,1f)
            }
        }
    }

    private fun playHarmony(step: Int) {
        val selectedIndex = spinnerProgression.selectedItemPosition

        // Se hai scelto "Nessun Piano" (che ora è all'indice 0), non suonare nulla
        if (selectedIndex == 0) return

        if (step % 4 != 0) return

        // Mappiamo l'indice dello spinner alla progressione corretta
        val progression = when (selectedIndex) {
            1 -> listOf("I", "V", "vi", "IV")
            2 -> listOf("ii", "V", "I")
            3 -> listOf("vi", "IV", "I", "V")
            else -> listOf("I", "V", "vi", "IV")
        }

        val chordDegree = progression[(step / 4) % progression.size]
        val (rootNote, chordType) = getChordFromDegree(key, chordDegree)

        playChord(rootNote, chordType)
    }

    private fun playChord(root: String, chordType: String) {
        val semitones = mapOf(
            "major" to listOf(0, 4, 7),
            "minor" to listOf(0, 3, 7),
            "dim"   to listOf(0, 3, 6)
        )

        val intervals = semitones[chordType] ?: return
        val noteOrder = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
        val rootIndex = noteOrder.indexOf(root)

        for (interval in intervals) {
            val idx = (rootIndex + interval) % 12
            val noteName = noteOrder[idx]
            val noteId = pianoNotes[noteName] ?: continue
            soundPool.play(noteId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun getChordFromDegree(keyRoot: String, degree: String): Pair<String, String> {
        val semitones = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
        val majorScale = listOf(0,2,4,5,7,9,11)

        val degreeMap = mapOf(
            "I" to 0, "ii" to 1, "iii" to 2, "IV" to 3,
            "V" to 4, "vi" to 5, "vii°" to 6
        )

        val keyIndex = semitones.indexOf(keyRoot)
        val degIndex = degreeMap[degree] ?: 0

        val rootNote = semitones[(keyIndex + majorScale[degIndex]) % 12]

        val type = when (degree) {
            "I","IV","V" -> "major"
            "ii","iii","vi" -> "minor"
            "vii°" -> "dim"
            else -> "major"
        }
        return Pair(rootNote, type)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAll()
        voicePlayer?.release()
        voicePlayer = null
        soundPool.release()
    }
}