package it.progmob.esame2

import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

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
    private var mediaPlayer: MediaPlayer? = null

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

    // VARIABILI MUSICALI DA INSERIRE QUANDO SONO PRESE DAL WEB
    private var bpm = 120
    private var key = "C"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arranger)

        txtKey = findViewById(R.id.txtKey)
        txtBpm = findViewById(R.id.txtBpm)
        spinnerProgression = findViewById(R.id.spinnerProgression)
        radioDrums = findViewById(R.id.radioDrums)
        btnPlay = findViewById(R.id.btnPlay)
        btnStop = findViewById(R.id.btnStop)

        // Get values from previous Activity
        key = intent.getStringExtra("key") ?: "C"
        bpm = intent.getIntExtra("bpm", 120)

        txtKey.text = "Key: $key"
        txtBpm.text = "BPM: $bpm"

        // Set progression choices
        val progressions = listOf("I - V - vi - IV", "ii - V - I", "vi - IV - I - V")
        spinnerProgression.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            progressions
        )

        // Setup SoundPool
        soundPool = SoundPool.Builder().setMaxStreams(12).build()

        loadPianoNotes()
        loadDrumSamples()

        // Load recorded audio (loop)
        val audioPath = intent.getStringExtra("audioPath")
        if (audioPath != null) {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                isLooping = true
                prepare()
            }
        }

        btnPlay.setOnClickListener { startAll() }
        btnStop.setOnClickListener { stopAll() }
    }

    // -------------------------------------------------------
    // LOAD SAMPLES
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
    // MAIN LOOP
    // -------------------------------------------------------

    private fun startAll() {
        if (isPlaying) return
        isPlaying = true
        step = 0

        mediaPlayer?.start()
        startLoop()
    }

    private fun stopAll() {
        isPlaying = false
        mediaPlayer?.pause()
        mediaPlayer?.seekTo(0)
    }

    private fun startLoop() {
        val stepMs = (60000f / bpm / 4f).toLong()

        handler.post(object : Runnable {
            override fun run() {
                if (!isPlaying) return

                playDrums(step)
                playHarmony(step)

                step = (step + 1) % 16
                handler.postDelayed(this, stepMs)
            }
        })
    }

    // -------------------------------------------------------
    // DRUM ENGINE
    // -------------------------------------------------------

    private fun playDrums(step: Int) {
        val style = when (radioDrums.checkedRadioButtonId) {
            R.id.radioPop -> "pop"
            R.id.radioRock -> "rock"
            R.id.radioReggae -> "reggae"
            else -> "pop"
        }

        when (style) {
            "pop" -> {
                if (step == 0 || step == 8) soundPool.play(kickId,1f,1f,1,0,1f)
                if (step == 4 || step == 12) soundPool.play(snareId,1f,1f,1,0,1f)
                soundPool.play(hihatId, 0.4f,0.4f,1,0,1f)
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

    // -------------------------------------------------------
    // HARMONY ENGINE (PIANO CHORDS)
    // -------------------------------------------------------

    private fun playHarmony(step: Int) {
        if (step % 4 != 0) return

        val progression = when (spinnerProgression.selectedItemPosition) {
            0 -> listOf("I", "V", "vi", "IV")
            1 -> listOf("ii", "V", "I")
            2 -> listOf("vi", "IV", "I", "V")
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
            "I" to 0,
            "ii" to 1,
            "iii" to 2,
            "IV" to 3,
            "V" to 4,
            "vi" to 5,
            "vii°" to 6
        )

        val keyIndex = semitones.indexOf(keyRoot)
        val degIndex = degreeMap[degree] ?: 0

        val rootNote = semitones[(keyIndex + majorScale[degIndex]) % 12]

        val type =
            when (degree) {
                "I","IV","V" -> "major"
                "ii","iii","vi" -> "minor"
                "vii°" -> "dim"
                else -> "major"
            }

        return Pair(rootNote, type)
    }
}
