package com.example.snakegame.presentation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import com.example.snakegame.data.Direction
import com.example.snakegame.data.GameListener
import com.example.snakegame.data.GameMode
import java.util.LinkedList
import java.util.Random
import java.util.logging.Handler
import kotlin.math.min

class SnakeGameView @JvmOverloads constructor(
    context:Context,
    attributeSet: AttributeSet,
    defStyleAttr: Int = 0
):View(context,attributeSet,defStyleAttr) {
    private val handler = android.os.Handler(Looper.getMainLooper())
    private val updateDelay = 150L // Tốc độ cập nhật (milliseconds)
    private var gridSize = 20 // Số ô trên mỗi trục
    private var cellSize = 0f // Kích thước mỗi ô, được tính dựa trên kích thước view
    private var currentLevel = GameMode.EASY

    private val snake = LinkedList<Pair<Int, Int>>() // Danh sách các phần tử của rắn
    private var food = Pair(0, 0) // Vị trí thức ăn
    private val obstacles = mutableListOf<Pair<Int, Int>>() // Danh sách vật cản


    private var direction = Direction.RIGHT // Hướng di chuyển ban đầu
    private var nextDirection = Direction.RIGHT // Hướng di chuyển tiếp theo

    var score = 0
        private set
    var lives = 2 // Người chơi bắt đầu với 2 mạng
        private set

    private var isGameRunning = false

    private val snakePaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private val foodPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val obstaclePaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private var gameListener: GameListener? = null
    init {
        resetGame()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = min(w, h) / gridSize.toFloat()

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Vẽ viền
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)
        // Vẽ vật cản
        for (obstacle in obstacles) {
            val obstacleX = obstacle.first * cellSize
            val obstacleY = obstacle.second * cellSize
            canvas.drawRect(obstacleX, obstacleY, obstacleX + cellSize, obstacleY + cellSize, obstaclePaint)
        }

        // Vẽ thức ăn
        val foodX = food.first * cellSize
        val foodY = food.second * cellSize
        canvas.drawRect(foodX, foodY, foodX + cellSize, foodY + cellSize, foodPaint)
        // Vẽ rắn
        for (segment in snake) {
            val segmentX = segment.first * cellSize
            val segmentY = segment.second * cellSize
            canvas.drawRect(segmentX, segmentY, segmentX + cellSize, segmentY + cellSize, snakePaint)
        }
    }
    private val updateRunnable = object :Runnable {
        override fun run() {
            if (isGameRunning) {
                update()
                invalidate()
                handler.postDelayed(this, updateDelay)
            }
        }
    }
    private fun update(){
        direction = nextDirection

        // Lấy vị trí đầu rắn
        val head = snake.first
        // Tính toán vị trí mới cho đầu rắn
        val newHead = when (direction) {
            Direction.UP -> Pair(head.first, head.second - 1)
            Direction.RIGHT -> Pair(head.first + 1, head.second)
            Direction.DOWN -> Pair(head.first, head.second + 1)
            Direction.LEFT -> Pair(head.first - 1, head.second)
        }
        // Kiểm tra va chạm với tường
        if (newHead.first < 0 || newHead.first >= gridSize ||
            newHead.second < 0 || newHead.second >= gridSize) {
            handleCollision()
            return
        }
        // Kiểm tra va chạm với chính mình
        if (snake.contains(newHead)) {
            handleCollision()
            return
        }
        // Thêm đầu mới
        snake.addFirst(newHead)

        // Kiểm tra xem có ăn được thức ăn không
        if (newHead == food) {
            // Tăng điểm và tạo thức ăn mới
            score++
            gameListener?.onScoreChanged(score)
            generateFood()
        } else {
            // Nếu không ăn được thức ăn, xóa đuôi để giữ độ dài không đổi
            snake.removeLast()
        }
        // Kiểm tra nếu điểm đạt 10 và đang ở level dễ thì chuyển sang level khó
        if (score == 10 && currentLevel == GameMode.EASY) {
            currentLevel = GameMode.HARD
            gameListener?.onLevelChanged(currentLevel)
            generateObstacles()
        }
    }
    private fun generateObstacles(){
        obstacles.clear()
        val random = Random()

        // Tạo 8 vật cản ngẫu nhiên
        val obstacleCount = 8
        for (i in 0 until obstacleCount) {
            var obstacle: Pair<Int, Int>
            do {
                obstacle = Pair(random.nextInt(gridSize), random.nextInt(gridSize))
                // Đảm bảo vật cản không trùng với rắn, thức ăn, và các vật cản khác
            } while (snake.contains(obstacle) || obstacle == food || obstacles.contains(obstacle))

            obstacles.add(obstacle)
        }
    }
    private fun handleCollision() {
        lives--
        gameListener?.onLivesChanged(lives)

        if (lives <= 0) {
            stopGame()
            gameListener?.onGameOver()
        } else {
            // Reset vị trí rắn nhưng giữ nguyên điểm số
            resetSnakePosition()
        }
    }
    private fun resetSnakePosition() {
        snake.clear()
        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT

        // Tạo rắn ban đầu với 3 đoạn
        val startX = gridSize / 2
        val startY = gridSize / 2
        snake.add(Pair(startX, startY))
        snake.add(Pair(startX - 1, startY))
        snake.add(Pair(startX - 2, startY))

        invalidate()
    }
    private fun generateFood() {
        val random = Random()
        var newFood: Pair<Int, Int>

        // Tạo vị trí thức ăn mới không trùng với vị trí của rắn
        do {
            newFood = Pair(random.nextInt(gridSize), random.nextInt(gridSize))
        } while (snake.contains(newFood)|| obstacles.contains(newFood))

        food = newFood
    }
    fun changeDirection(newDirection: Direction) {
        // Ngăn rắn quay đầu 180 độ
        nextDirection = when (direction) {
            Direction.UP -> if (newDirection != Direction.DOWN) newDirection else direction
            Direction.RIGHT -> if (newDirection != Direction.LEFT) newDirection else direction
            Direction.DOWN -> if (newDirection != Direction.UP) newDirection else direction
            Direction.LEFT -> if (newDirection != Direction.RIGHT) newDirection else direction
        }
    }
    fun startGame() {
        if (!isGameRunning) {
            isGameRunning = true
            handler.post(updateRunnable)
        }
    }
    fun pauseGame() {
        isGameRunning = false
        handler.removeCallbacks(updateRunnable)
    }
    fun resumeGame() {
        if (!isGameRunning) {
            isGameRunning = true
            handler.post(updateRunnable)
        }
    }

    fun stopGame() {
        isGameRunning = false
        handler.removeCallbacks(updateRunnable)
    }
    fun resetGame() {
        score = 0
        lives = 2
        gameListener?.onScoreChanged(score)
        gameListener?.onLivesChanged(lives)

        resetSnakePosition()
        generateFood()
        startGame()
    }
    fun setOnGameListener(listener: GameListener) {
        this.gameListener = listener
    }
}