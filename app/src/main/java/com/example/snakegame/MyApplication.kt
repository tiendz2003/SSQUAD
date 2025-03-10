package com.example.snakegame

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.snakegame.ads.AppOpenManager
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MyApplication: Application(), Application.ActivityLifecycleCallbacks  {
    private lateinit var appOpenManager: AppOpenManager
    private var currentActivity: Activity? = null
    private var isAppInForeground = false
    override fun onCreate() {
        super.onCreate()
        // Chạy Mobile Ads SDK trên một luồng nền để tránh chặn Main Thread
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MyApplication) {}
        }
        registerActivityLifecycleCallbacks(this)
        // Khởi tạo App Open Ad Manager
        appOpenManager = AppOpenManager()
        appOpenManager.loadAd(this)

        // Lắng nghe sự kiện Lifecycle để hiển thị quảng cáo khi app vào foreground
    }

    /** Hiển thị quảng cáo khi ứng dụng được đưa lên foreground */
    fun showAdIfAvailable() {
        if (currentActivity != null && isAppInForeground) {
            Log.d("MyApplication", "Hiển thị quảng cáo cho: ${currentActivity}")
            appOpenManager.showAdIfAvailable(currentActivity!!)
        }
    }
    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?
    ) {

    }

    override fun onActivityStarted(activity: Activity) {
        Log.d("MyApplication", "Started")
        Log.d("MyApplication", "App vào foreground: $activity")
        currentActivity = activity
        isAppInForeground = true
        // Gọi showAdIfAvailable() ngay khi app vào foreground
        showAdIfAvailable()
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d("MyApplication", "onActivityResumed")
       currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d("MyApplication", "onActivityPaused")
    }

    override fun onActivityStopped(activity: Activity) {
        if (activity == currentActivity) {
            isAppInForeground = false
        }
    }
    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }
}