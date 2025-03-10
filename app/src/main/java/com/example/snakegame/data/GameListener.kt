package com.example.snakegame.data

interface GameListener {
    fun onScoreChanged(score: Int)
    fun onLivesChanged(lives: Int)
    fun onGameOver()
    fun onLevelChanged(level: GameMode)
}