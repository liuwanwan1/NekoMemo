package mirujam.nekomemo.ui.test

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mirujam.nekomemo.data.local.Converters
import mirujam.nekomemo.data.local.entity.QuestionEntity
import mirujam.nekomemo.data.preferences.ThemePreferenceRepository
import mirujam.nekomemo.data.repository.QuestionRepository
import javax.inject.Inject

private const val TAG = "TestViewModel"

data class QuestionUiState(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

@HiltViewModel
class TestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuestionRepository,
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val converters: Converters
) : ViewModel() {

    private val bankId: Long = savedStateHandle["bankId"] ?: -1L
    private val questionCount: Int = savedStateHandle["questionCount"] ?: 0

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val questions: StateFlow<List<QuestionEntity>> = repository.getQuestionsForBank(bankId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directAnswer: StateFlow<Boolean> = themePreferenceRepository.directAnswer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _shuffledQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val shuffledQuestions: StateFlow<List<QuestionEntity>> = _shuffledQuestions.asStateFlow()

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val _bankTitle = MutableStateFlow("Test Mode")
    val bankTitle: StateFlow<String> = _bankTitle.asStateFlow()

    private val _selectedAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val selectedAnswers: StateFlow<Map<Int, Int>> = _selectedAnswers.asStateFlow()

    private val _revealedQuestions = MutableStateFlow<Set<Int>>(emptySet())
    val revealedQuestions: StateFlow<Set<Int>> = _revealedQuestions.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    private val _isReviewing = MutableStateFlow(false)
    val isReviewing: StateFlow<Boolean> = _isReviewing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var hasReceivedData = false
    private var isFirstEmission = true

    init {
        Log.d(TAG, "=== TestViewModel Created ===")
        Log.d(TAG, "bankId: $bankId")
        Log.d(TAG, "questionCount: $questionCount")

        viewModelScope.launch {
            val bank = repository.getBankById(bankId)
            _bankTitle.value = bank?.title ?: "Test Mode"
            Log.d(TAG, "Bank loaded: ${bank?.title}, ID: $bankId")
        }

        viewModelScope.launch {
            questions.collect { newList ->
                Log.d(TAG, "Questions state changed - size: ${newList.size}, hasReceivedData: $hasReceivedData, isFirstEmission: $isFirstEmission")

                if (isFirstEmission) {
                    isFirstEmission = false
                    Log.d(TAG, "Skipping initial StateFlow value (this is just the default emptyList)")
                    return@collect
                }

                if (!hasReceivedData) {
                    hasReceivedData = true
                    _isLoading.value = false
                    Log.d(TAG, "First REAL data received! isLoading set to false. Question count: ${newList.size}")
                }
            }
        }
    }

    fun getActiveQuestions(): List<QuestionEntity> {
        val source = if (_isShuffled.value && _shuffledQuestions.value.isNotEmpty()) {
            _shuffledQuestions.value
        } else {
            questions.value
        }
        val result = if (questionCount > 0 && questionCount < source.size) {
            source.take(questionCount)
        } else {
            source
        }
        Log.d(TAG, "getActiveQuestions() called - isShuffled: ${_isShuffled.value}, " +
                "sourceSize: ${source.size}, questionCount: $questionCount, resultSize: ${result.size}")
        return result
    }

    fun toggleShuffle() {
        _isShuffled.value = !_isShuffled.value
        if (_isShuffled.value) {
            _shuffledQuestions.value = questions.value.shuffled()
        } else {
            _shuffledQuestions.value = emptyList()
        }
        resetTest()
    }

    fun toUiState(entity: QuestionEntity): QuestionUiState {
        return QuestionUiState(
            text = entity.text,
            options = converters.toStringList(entity.options),
            correctIndex = entity.correctIndex
        )
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        _selectedAnswers.value = _selectedAnswers.value + (questionIndex to optionIndex)
        if (directAnswer.value) {
            revealAnswer(questionIndex)
        }
    }

    fun revealAnswer(questionIndex: Int) {
        _revealedQuestions.value = _revealedQuestions.value + questionIndex
    }

    fun nextQuestion(total: Int) {
        if (_currentIndex.value < total - 1) {
            _currentIndex.value += 1
        }
    }

    fun previousQuestion() {
        if (_currentIndex.value > 0) {
            _currentIndex.value -= 1
        }
    }

    fun finishTest() {
        _isFinished.value = true
    }

    fun startReview() {
        _isReviewing.value = true
        _currentIndex.value = 0
    }

    fun exitReview() {
        _isReviewing.value = false
    }

    fun resetTest() {
        _selectedAnswers.value = emptyMap()
        _revealedQuestions.value = emptySet()
        _currentIndex.value = 0
        _isFinished.value = false
        _isReviewing.value = false
        if (_isShuffled.value) {
            _shuffledQuestions.value = questions.value.shuffled()
        }
    }

    fun calculateScore(questions: List<QuestionEntity>): ScoreResult {
        var correct = 0
        var wrong = 0
        var unanswered = 0
        questions.forEachIndexed { index, entity ->
            val selected = _selectedAnswers.value[index]
            if (selected == null) {
                unanswered++
            } else if (selected == entity.correctIndex) {
                correct++
            } else {
                wrong++
            }
        }
        return ScoreResult(correct, wrong, unanswered, questions.size)
    }
}

data class ScoreResult(
    val correct: Int,
    val wrong: Int,
    val unanswered: Int,
    val total: Int
)
