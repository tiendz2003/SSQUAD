package com.example.snakegame

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.ViewModelProvider
import com.example.snakegame.data.Direction
import com.example.snakegame.data.GameListener
import com.example.snakegame.data.GameMode
import com.example.snakegame.databinding.ActivitySnakeGameBinding
import com.example.snakegame.presentation.SnakeGameView
import com.example.snakegame.presentation.SnakeViewModel
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.math.abs


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySnakeGameBinding
    private lateinit var viewModel: SnakeViewModel
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var adView: AdView
    private lateinit var adRequest: AdRequest
    private var mInterstitialAd: InterstitialAd ?= null
    private var mRewardedAd: RewardedAd ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySnakeGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adRequest = AdRequest.Builder().build()
        MobileAds.initialize(this) {}
        adView = binding.adView
        //Khởi tạo interstitial ad
        loadInterstitialAd()
        loadRewardAd()
        adView.loadAd(adRequest)
        viewModel = ViewModelProvider(this)[SnakeViewModel::class.java]
        setupGame()
        observeGameEvents()
        updateLevel(GameMode.EASY)
    }

    private fun setupGame() {
        binding.apply {
            gameView.setOnGameListener(object : GameListener {
                override fun onScoreChanged(score: Int) = viewModel.updateScore(score)
                override fun onLivesChanged(lives: Int) = viewModel.updateLives(lives)
                override fun onGameOver(){
                    showGameOverDialog()
                    loadInterstitialAd()
                    loadRewardAd()
                }
                override fun onLevelChanged(level: GameMode) {
                    showLevelUpMessage()
                    updateLevel(level)
                }
            })

            pauseButton.setOnClickListener { togglePause() }
            gestureDetector = GestureDetectorCompat(this@MainActivity, SwipeGestureListener())
        }
    }

    private fun observeGameEvents() {
        viewModel.score.observe(this) { binding.scoreTextView.text = "Điểm: $it" }
        viewModel.lives.observe(this) { binding.livesTextView.text = "Mạng: $it" }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return event?.let { gestureDetector.onTouchEvent(it) } == true || super.onTouchEvent(event)
    }

    private fun togglePause() {
        with(binding) {
            if (viewModel.isPaused.value == true) {
                gameView.resumeGame()
                pauseButton.text = "Tạm dừng"
            } else {
                gameView.pauseGame()
                pauseButton.text = "Tiếp tục"
            }
            viewModel.togglePause()
        }
    }
    private fun showLevelUpMessage(){
        Toast.makeText(
            this,
            "Chúc mừng! Bạn đã đạt 10 điểm và chuyển sang màn chơi khó hơn với vật cản.",
            Toast.LENGTH_LONG
        ).show()
        binding.gameView.pauseGame()
        Handler(Looper.getMainLooper()).postDelayed({
            if(viewModel.isPaused.value == false) binding.gameView.resumeGame()
        },1500)
    }
    private fun updateLevel(level: GameMode) {
        val levelText = when(level) {
            GameMode.EASY -> "Dễ"
            GameMode.HARD -> "Khó"
        }
        binding.levelTextView.text = "Cấp độ: $levelText"
    }

    private fun showGameOverDialog() {
        AlertDialog.Builder(this)
            .setTitle("Trò chơi kết thúc")
            .setMessage("Điểm của bạn: ${viewModel.score.value}")
            .setPositiveButton("Chơi lại") { _, _ -> showInterstitialAd()}
            .setNegativeButton("Thoát") { _, _ -> finish() }
            .setNeutralButton("Xem ads để thêm mạng") { _, _ -> showRewardedAd() }
            .setCancelable(false)
            .show()
    }
    private fun showInterstitialAd(){
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null // Đặt lại biến sau khi quảng cáo đóng
                    loadInterstitialAd() // Tải quảng cáo mới
                    binding.gameView.resetGame() // Khởi động lại trò chơi
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    binding.gameView.resetGame()
                }
            }
            //show quảng cáo
            mInterstitialAd?.show(this)
        }
    }
    private fun showRewardedAd() {
        if (mRewardedAd != null) {
            mRewardedAd?.show(this) {rewardItem->
                val rewardAmount = rewardItem.amount
                viewModel.addExtraLives(rewardAmount)
                binding.gameView.resetGame()
                loadRewardAd()
            }
        }
    }
    //tải interstitial ad
    private fun loadInterstitialAd() {
        InterstitialAd.load(
            this,
            "ca-app-pub-3940256099942544/1033173712",
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    Log.d("MainActivity", adError.toString().toString())
                    mInterstitialAd = null
                }
            }
        )
    }
    //tải rewardAd
    private fun loadRewardAd(){
        RewardedAd.load(
            this,
            "ca-app-pub-3940256099942544/5224354917",
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    mRewardedAd = null
                }
            }
        )
    }
    override fun onPause() {
        super.onPause()
        binding.gameView.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isPaused.value == false) {
            binding.gameView.resumeGame()
        }
    }

    private inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            e1 ?: return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y

            binding.gameView.changeDirection(
                when {
                    abs(diffX) > abs(diffY) -> if (diffX > 0) Direction.RIGHT else Direction.LEFT
                    else -> if (diffY > 0) Direction.DOWN else Direction.UP
                }
            )
            return true
        }
    }
}




