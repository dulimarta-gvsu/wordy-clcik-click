package edu.gvsu.cis.worder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlin.collections.map

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


    /////////////////////////
    private val _targetLetters = MutableStateFlow<List<Letter?>>(emptyList())
    val targetLetters: StateFlow<List<Letter?>> = _targetLetters.asStateFlow()

    val currentWord: StateFlow<String> =
        targetLetters
            .map { letters ->
                letters
                    .filterNotNull()
                    .joinToString("") { it.text.toString() }
                    .uppercase()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = ""
            )

    val isValidWord: StateFlow<Boolean> =
        currentWord
            .map { word -> _dictionary.contains(word) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = false
            )
    /////////////////////////


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
        _currentScore.value = 0
    }

    // One button to reshuffle the remaining stock letters
    fun reshuffle() {
        _sourceLetters.update { it.shuffled() }
    }

    // One button to record the word and add the score to the current total.
    fun addWord() {
        // check if the word is valid
        // comment out for now
//        if (!_isValidWord.value) return

        val word = currentWord.value
//        println("Current word: $word")
        val score = _currentScore.value
//        println("Current word score: $score")

        _wordBuiltSofar.update { currentMap ->
            if (currentMap.contains(word)) {
                currentMap
            } else {
                currentMap + (word to score)
            }
        }

        _totalScore.update { it + score }
//        println("Total score ${_totalScore.value}")
//        println("map : ${_wordBuiltSofar.value}")
    }

    fun rearrangeLetters(group: Origin, arr: List<Letter?>) {
        when (group) {
            Origin.Stock -> {
                _sourceLetters.update {
                    arr
                }
            }
            Origin.CenterBox -> {
                _targetLetters.update {
                    arr
                }
                calculateCurrentScore(arr)
            }
        }
    }

    // this calculate immediately after rearranging takes place
    // can not use .Eagerly
    fun calculateCurrentScore(target: List<Letter?>) {
        val letters = target.filterNotNull()

        val word = letters
            .joinToString("") { it.text.toString() }
            .uppercase()

        // for checking if the word is valid
        // there are not many words in dict, so turning this off
//        if (!_dictionary.contains(word)) {
//            _currentScore.value = 0
//            return
//        }

        val letterSum = letters.sumOf { letter ->
            letter.point * letter.letterMl
        }

        val wordMultiplier = letters.fold(1) { acc, letter ->
            acc * letter.wordMl
        }

        _currentScore.value = letterSum * wordMultiplier
    }

}
