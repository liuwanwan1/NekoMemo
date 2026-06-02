package mirujam.nekomemo.ui.test

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mirujam.nekomemo.R
import mirujam.nekomemo.domain.model.Question
import mirujam.nekomemo.data.preferences.TestPreferenceRepository
import mirujam.nekomemo.data.repository.QuestionRepository
import mirujam.nekomemo.data.repository.TestHistoryRepository
import mirujam.nekomemo.ui.model.QuestionUiModel
import mirujam.nekomemo.ui.model.ScoreModel
import mirujam.nekomemo.ui.model.UiText
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuestionRepository,
    private val testHistoryRepository: TestHistoryRepository,
    testPreferenceRepository: TestPreferenceRepository
) : ViewModel() {

    private val bankId: Long = savedStateHandle["bankId"] ?: -1L
    private val questionCount: Int = savedStateHandle["questionCount"] ?: 0
    private val shuffleQuestions: Boolean = savedStateHandle["shuffleQuestions"] ?: false
    private val shuffleOptions: Boolean = savedStateHandle["shuffleOptions"] ?: false

    private val questions: StateFlow<List<Question>> = repository.getQuestionsForBank(bankId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questionUiModels: StateFlow<List<QuestionUiModel>> = questions.map { domainQuestions ->
        val models = QuestionUiModel.fromDomainModels(domainQuestions)
        if (shuffleOptions) {
            models.map { model ->
                val shuffledOptions = model.options.shuffled()
                val newCorrectIndex = shuffledOptions.indexOf(model.options[model.correctIndex])
                model.copy(options = shuffledOptions, correctIndex = newCorrectIndex)
            }
        } else {
            models
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directAnswer: StateFlow<Boolean> = testPreferenceRepository.directAnswer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _shuffledQuestions = MutableStateFlow<List<QuestionUiModel>>(emptyList())

    private val _isShuffled = MutableStateFlow(shuffleQuestions)

    private val _bankTitle = MutableStateFlow<UiText>(UiText.StringResource(R.string.test_mode_title))
    val bankTitle: StateFlow<UiText> = _bankTitle.asStateFlow()

    private val _selectedAnswers = MutableStateFlow(emptyMap<Int, Set<Int>>())
    val selectedAnswers: StateFlow<Map<Int, Set<Int>>> = _selectedAnswers.asStateFlow()

    private val _revealedQuestions = MutableStateFlow(emptySet<Int>())
    val revealedQuestions: StateFlow<Set<Int>> = _revealedQuestions.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    private val _isReviewing = MutableStateFlow(false)
    val isReviewing: StateFlow<Boolean> = _isReviewing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var testStartTime: Long = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            val bank = repository.getBankById(bankId)
            _bankTitle.value = bank?.title?.let { UiText.DynamicString(it) }
                ?: UiText.StringResource(R.string.test_mode_title)
        }

        viewModelScope.launch {
            try {
                val models = questionUiModels.first { it.isNotEmpty() }
                _isLoading.value = false
                if (shuffleQuestions) {
                    _shuffledQuestions.value = models.shuffled()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading questions for test")
                _isLoading.value = false
            }
        }
    }

    fun getActiveQuestions(): List<QuestionUiModel> {
        val shuffled = _shuffledQuestions.value
        val source = if (_isShuffled.value && shuffled.isNotEmpty()) {
            shuffled
        } else {
            questionUiModels.value
        }
        
        val count = if (questionCount > 0) questionCount else source.size
        return source.take(count)
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val questions = getActiveQuestions()
        val question = questions.getOrNull(questionIndex) ?: return
        val shouldReveal = directAnswer.value

        _selectedAnswers.update { current ->
            val mutable = current.toMutableMap()
            if (question.isMultipleChoice) {
                val existing = mutable[questionIndex] ?: emptySet()
                mutable[questionIndex] = if (optionIndex in existing) {
                    existing - optionIndex
                } else {
                    existing + optionIndex
                }
            } else {
                mutable[questionIndex] = setOf(optionIndex)
            }
            mutable
        }
        if (shouldReveal && !question.isMultipleChoice) {
            revealAnswer(questionIndex)
        }
    }

    fun confirmMultipleChoice(questionIndex: Int) {
        revealAnswer(questionIndex)
    }

    fun revealAnswer(questionIndex: Int) {
        _revealedQuestions.update { it.toMutableSet().apply { add(questionIndex) } }
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
        recordTestResults()
    }

    private fun recordTestResults() {
        viewModelScope.launch {
            try {
                val questions = getActiveQuestions()
                val score = calculateScore(questions)
                val durationMs = System.currentTimeMillis() - testStartTime

                // Record test session
                testHistoryRepository.recordTestSession(
                    bankId = bankId,
                    totalQuestions = score.total,
                    correctCount = score.correct,
                    wrongCount = score.wrong,
                    unansweredCount = score.unanswered,
                    percentage = score.percentage,
                    durationMs = durationMs
                )

                // Record wrong questions
                questions.forEachIndexed { index, question ->
                    val selected = _selectedAnswers.value[index]
                    val isCorrect = when {
                        selected == null || selected.isEmpty() -> false
                        question.isMultipleChoice -> selected == question.correctIndices.toSet()
                        else -> selected.size == 1 && selected.first() == question.correctIndex
                    }

                    if (!isCorrect && selected != null && selected.isNotEmpty()) {
                        testHistoryRepository.recordWrongQuestion(question.id, bankId)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error recording test results")
            }
        }
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
        testStartTime = System.currentTimeMillis()
        if (_isShuffled.value) {
            _shuffledQuestions.value = questionUiModels.value.shuffled()
        } else {
            _shuffledQuestions.value = emptyList()
        }
    }

    fun calculateScore(questions: List<QuestionUiModel>): ScoreModel {
        return ScoreModel.calculate(questions, _selectedAnswers.value)
    }

    fun getSelectedOptions(questionIndex: Int): Set<Int> {
        return _selectedAnswers.value[questionIndex] ?: emptySet()
    }
}
