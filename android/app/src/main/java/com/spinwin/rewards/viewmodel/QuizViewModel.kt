package com.spinwin.rewards.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spinwin.rewards.data.model.QuizCategory
import com.spinwin.rewards.data.model.QuizQuestion
import com.spinwin.rewards.data.repository.RewardsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class QuizGameState {
    CATEGORIES, PLAYING, RESULT
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RewardsRepository.getInstance(application)

    val categories: List<QuizCategory> = repository.getQuizCategories()
    val activeCountry: StateFlow<com.spinwin.rewards.data.model.CountryInfo> = repository.activeCountry

    private val _selectedCategory = MutableStateFlow<QuizCategory?>(null)
    val selectedCategory: StateFlow<QuizCategory?> = _selectedCategory.asStateFlow()

    private val _gameState = MutableStateFlow(QuizGameState.CATEGORIES)
    val gameState: StateFlow<QuizGameState> = _gameState.asStateFlow()

    private val _currentQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val currentQuestions: StateFlow<List<QuizQuestion>> = _currentQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedOption = MutableStateFlow<Int?>(null)
    val selectedOption: StateFlow<Int?> = _selectedOption.asStateFlow()

    private val _isAnswerSubmitted = MutableStateFlow(false)
    val isAnswerSubmitted: StateFlow<Boolean> = _isAnswerSubmitted.asStateFlow()

    private val _isCurrentCorrect = MutableStateFlow(false)
    val isCurrentCorrect: StateFlow<Boolean> = _isCurrentCorrect.asStateFlow()

    private val _timerProgress = MutableStateFlow(1.0f) // 1.0 down to 0.0
    val timerProgress: StateFlow<Float> = _timerProgress.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _totalAnsweredSession = MutableStateFlow(0)
    val totalAnsweredSession: StateFlow<Int> = _totalAnsweredSession.asStateFlow()

    private val _eliminatedOptions = MutableStateFlow<Set<Int>>(emptySet())
    val eliminatedOptions: StateFlow<Set<Int>> = _eliminatedOptions.asStateFlow()

    private val _activeHint = MutableStateFlow<String?>(null)
    val activeHint: StateFlow<String?> = _activeHint.asStateFlow()

    private var timerJob: Job? = null

    fun selectCategory(category: QuizCategory) {
        _selectedCategory.value = category
        val questions = repository.getQuestionsForCategory(category.id)
        _currentQuestions.value = questions
        _currentQuestionIndex.value = 0
        _score.value = 0
        _gameState.value = QuizGameState.PLAYING
        startQuestion()
    }

    /**
     * Top Back Button: Return to categories at any time
     */
    fun goBackToCategories() {
        timerJob?.cancel()
        _gameState.value = QuizGameState.CATEGORIES
    }

    private fun startQuestion() {
        _selectedOption.value = null
        _isAnswerSubmitted.value = false
        _eliminatedOptions.value = emptySet()
        _activeHint.value = null
        _timerProgress.value = 1.0f

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val totalSeconds = 15
            for (i in 0 until (totalSeconds * 10)) {
                delay(100)
                _timerProgress.value = 1.0f - (i.toFloat() / (totalSeconds * 10))
            }
            if (!_isAnswerSubmitted.value) {
                // Time up
                submitAnswer(-1)
            }
        }
    }

    fun submitAnswer(optionIndex: Int) {
        if (_isAnswerSubmitted.value) return
        timerJob?.cancel()

        _selectedOption.value = optionIndex
        _isAnswerSubmitted.value = true

        val currentQ = _currentQuestions.value.getOrNull(_currentQuestionIndex.value)
        val isCorrect = optionIndex == currentQ?.correctIndex
        _isCurrentCorrect.value = isCorrect

        if (isCorrect) {
            _score.value += 1
            _totalAnsweredSession.value += 1
            // Instantly credit points for every correct answer (Unlimited Earning!)
            repository.creditPoints(20, "Correct Quiz Answer (+20 Pts)", "🧠", "quiz")
        }

        // Auto-advance after 1.2 seconds delay to show feedback
        viewModelScope.launch {
            delay(1200)
            if (_currentQuestionIndex.value + 1 < _currentQuestions.value.size) {
                _currentQuestionIndex.value += 1
                startQuestion()
            } else {
                // Load more questions automatically for UNLIMITED quiz gameplay!
                val catId = _selectedCategory.value?.id ?: "tech"
                val moreQuestions = repository.getQuestionsForCategory(catId)
                _currentQuestions.value = moreQuestions
                _currentQuestionIndex.value = 0
                _gameState.value = QuizGameState.RESULT
            }
        }
    }

    fun continuePlayingNextRound() {
        val catId = _selectedCategory.value?.id ?: "tech"
        val moreQuestions = repository.getQuestionsForCategory(catId)
        _currentQuestions.value = moreQuestions
        _currentQuestionIndex.value = 0
        _gameState.value = QuizGameState.PLAYING
        startQuestion()
    }

    fun useLifelineFiftyFifty(): Boolean {
        val currentQ = _currentQuestions.value.getOrNull(_currentQuestionIndex.value) ?: return false
        if (_eliminatedOptions.value.isNotEmpty()) return false

        val wrongIndices = currentQ.options.indices.filter { it != currentQ.correctIndex }
        _eliminatedOptions.value = wrongIndices.shuffled().take(2).toSet()
        return true
    }

    fun useLifelineSkip() {
        if (_currentQuestionIndex.value + 1 < _currentQuestions.value.size) {
            _currentQuestionIndex.value += 1
            startQuestion()
        } else {
            continuePlayingNextRound()
        }
    }

    fun useLifelineHint() {
        val currentQ = _currentQuestions.value.getOrNull(_currentQuestionIndex.value)
        _activeHint.value = currentQ?.hint ?: "Read carefully!"
    }

    fun claimDoubleRewardVideo(onRewarded: (Int) -> Unit) {
        viewModelScope.launch {
            val bonus = (_score.value * 20).coerceAtLeast(60)
            repository.watchAdReward()
            repository.creditPoints(bonus, "⚡ 2X Video Bonus for Quiz Round", "🏆", "quiz_bonus", bonus * 0.001)
            onRewarded(bonus)
        }
    }
}
