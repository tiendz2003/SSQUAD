package com.example.snakegame.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SnakeViewModel:ViewModel() {
    private val _score = MutableLiveData(0)
    val score: LiveData<Int> get() = _score

    private val _lives = MutableLiveData(2)
    val lives: LiveData<Int> get() = _lives

    private val _isPaused = MutableLiveData(false)
    val isPaused: LiveData<Boolean> get() = _isPaused

    fun updateScore(newScore: Int) { _score.value = newScore }
    fun updateLives(newLives: Int) { _lives.value = newLives }
    fun togglePause() { _isPaused.value = !_isPaused.value!! }
    fun addExtraLives(amount: Int) {
        _lives.value = (_lives.value ?: 0) + amount
    }
}