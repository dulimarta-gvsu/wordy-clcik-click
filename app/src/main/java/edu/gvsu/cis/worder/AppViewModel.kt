package edu.gvsu.cis.worder

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Letter(
    val text: Char = '$',
    val point: Int = 0,
    // Letter multiplier multiplies the point of individual letter in calculating the score
    val letterMl: Int = 1,
    // Word multiplier multiplies the total score of the word whenever this letter is used for building a word
    val wordMl: Int = 1,
)

enum class Origin {
    Stock, CenterBox
}

class AppViewModel : ViewModel() {
    private val _dictionary: Set<String> = setOf(
        // 3 letters
        "CAT", "DOG", "SUN", "CAR", "MAP", "PEN", "BOX", "HAT", "BAT", "BED",

        // 4 letters
        "TREE", "FISH", "BOOK", "MOON", "STAR", "WIND", "FIRE", "SHIP", "ROAD",

        // 5 letters
        "APPLE", "WATER", "HOUSE", "PLANT", "TRAIN", "CLOUD", "LIGHT", "SMILE",

        // 6 letters
        "FLOWER", "BRIDGE", "STREAM", "MARKET", "GARDEN", "POCKET",

        // 7 letters
        "ANIMAL", "FREEDOM", "BALANCE", "COUNTRY", "FAMILY",

        // 8 letters
        "COMPUTER", "ELEPHANT", "HOSPITAL", "NOTEBOOK"
    )

    private val _letterPoint: Map<Char, Int> = ('A'..'Z').associateWith { (1..10).random() }
    private val _sourceLetters = MutableStateFlow(emptyList<Letter?>())
    val sourceLetters = _sourceLetters.asStateFlow()
    private val _targetLetters = MutableStateFlow(emptyList<Letter?>())
    val targetLetters = _targetLetters.asStateFlow()

    private val _currentScore = MutableStateFlow(0)
    val currentScore = _currentScore.asStateFlow()

    private val _totalScore = MutableStateFlow(0)
    val totalScore = _totalScore.asStateFlow()

    private val _wordBuiltSofar = MutableStateFlow<Map<String, Int>>(emptyMap())
    val wordBuiltSofar = _wordBuiltSofar.asStateFlow()


    init {
        selectRandomLetters()
    }

    fun selectRandomLetters() {
        _sourceLetters.update {
            // 60% vowels, 40% consonants
            val vowels = (1..6).map { "AEIOU".random() }
            val consonants = (1..4).map { "BCFGHJKLMNPQRSTVWXYZ".random() }
            (vowels + consonants).map { ch ->
                val basePoint = _letterPoint[ch] ?: 0

                // 10% chance
                val hasMultiplier = (1..2).random() == 1

                val (letterMl, wordMl) = if (!hasMultiplier) 1 to 1
                else if ((0..1).random() == 0) (2..4).random() to 1
                else 1 to (2..4).random()

//                println("char: $ch - letterMl: $letterMl - wordMl: $wordMl")

                Letter(
                    text = ch, point = basePoint, letterMl = letterMl, wordMl = wordMl
                )

            }.shuffled()
        }
        _targetLetters.update { emptyList() }
    }
    fun rearrangeLetters(group: Origin, arr: List<Letter?>) {
        when (group) {
            Origin.Stock -> {
                _sourceLetters.update {
                    arr
                }
            }

            Origin.CenterBox -> {
                calculateCurrentScore(arr)
                _targetLetters.update {
                    arr
                }
            }
        }
    }

    fun lettersToWord(letters: List<Letter?>): String {
        return letters
            .filterNotNull()
            .map { it.text }
            .joinToString(separator = "")
            .uppercase()
    }

    fun addWord(word:String, score: Int) {
        _wordBuiltSofar.update { currentMap ->
            if (currentMap.contains(word)) {
                currentMap
            } else {
                currentMap + (word to score)
            }
        }
        println("map : ${_wordBuiltSofar.value}")
    }

    fun calculateCurrentScore(target: List<Letter?>) {
        val word = lettersToWord(target)
        val isValid = _dictionary.contains(word)

//        println("Word : $word")
//        println("IsValid : $isValid")

        // testing, should be !isValid
        if (isValid) {
//            println("Invalid word")
            return
        }

        val letters = target.filterNotNull()

        var wordMultiplier = 1

        val letterSum = letters.sumOf { letter ->
            wordMultiplier *= letter.wordMl
            letter.point * letter.letterMl
        }

        val totalScore = letterSum * wordMultiplier

        addWord(word, totalScore)
        _currentScore.update { totalScore }

//        println("Current score : ${_currentScore.value}")
    }

    fun calculateTotalScore () {
        _totalScore.value += _currentScore.value
//        println("Total score: ${_totalScore.value}")
    }

}
