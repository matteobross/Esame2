package it.progmob.esame2

object HarmonyEngine {

    // C, C#, D, ... B
    private val NOTE_NAMES = listOf(
        "C", "C#", "D", "D#", "E", "F",
        "F#", "G", "G#", "A", "A#", "B"
    )

    // Scale in semitoni dalla tonica (maggiore e minore naturale)
    private val MAJOR_SCALE = listOf(0, 2, 4, 5, 7, 9, 11)
    private val MINOR_SCALE = listOf(0, 2, 3, 5, 7, 8, 10)

    /**
     * key: "C major", "A minor", ecc.
     * degree: "I", "V", "vi", "ii", "vii°"...
     * Restituisce note MIDI di un triad (3 note)
     */
    fun getChordNotes(key: String, degree: String): List<Int> {
        val (rootName, isMajorKey) = parseKey(key)
        val rootIndex = NOTE_NAMES.indexOf(rootName)
        if (rootIndex == -1) return emptyList()

        // Partiamo da C3 = 48 e saliamo in base alla tonica
        val baseC3 = 48
        val rootMidi = baseC3 + (rootIndex - NOTE_NAMES.indexOf("C"))

        val scale = if (isMajorKey) MAJOR_SCALE else MINOR_SCALE

        val degreeNumber = romanToNumber(degree)
        if (degreeNumber !in 1..7) return emptyList()

        val scaleOffset = scale[degreeNumber - 1]
        val chordRoot = rootMidi + scaleOffset

        val chordQuality = when {
            degree.contains("°") -> ChordQuality.DIMINISHED
            degree.any { it.isLowerCase() } -> ChordQuality.MINOR
            else -> ChordQuality.MAJOR
        }

        val intervals = when (chordQuality) {
            ChordQuality.MAJOR -> listOf(0, 4, 7)
            ChordQuality.MINOR -> listOf(0, 3, 7)
            ChordQuality.DIMINISHED -> listOf(0, 3, 6)
        }

        return intervals.map { chordRoot + it }
    }

    private fun parseKey(key: String): Pair<String, Boolean> {
        // es: "C major", "A minor"
        val parts = key.trim().split(" ")
        if (parts.size < 2) return "C" to true

        val root = parts[0].replace("m", "m") // lasciamo così
        val quality = parts[1].lowercase()

        val normalizedRoot = NOTE_NAMES.find { it.equals(root, ignoreCase = true) } ?: "C"
        val isMajor = quality.contains("maj") || quality == "major"

        return normalizedRoot to isMajor
    }

    private fun romanToNumber(roman: String): Int {
        // rimuove simboli tipo "°"
        val clean = roman.replace("°", "").uppercase()
        return when (clean) {
            "I" -> 1
            "II" -> 2
            "III" -> 3
            "IV" -> 4
            "V" -> 5
            "VI" -> 6
            "VII" -> 7
            else -> 1
        }
    }

    private enum class ChordQuality {
        MAJOR, MINOR, DIMINISHED
    }
}
