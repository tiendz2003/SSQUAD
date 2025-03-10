package com.example.snakegame.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

private const val LOG_TAG = "AppOpenAdManager"
private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921"

class AppOpenManager {
    private var appOpenAd: AppOpenAd? = null
    private var isShowingAd = false
    private var isLoadingAd =false
    private var loadTime: Long = 0

    fun isAdAvailable(): Boolean{
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }
    fun loadAd(context: Context){
        //tải quảng cáo khi săn sàng
        if(isLoadingAd || isAdAvailable()){
            return
        }
        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            AD_UNIT_ID,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object: AppOpenAd.AppOpenAdLoadCallback(){
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()
                    Log.d(LOG_TAG, "Quảng cáo đã được tải")
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    isLoadingAd = false
                    Log.d(LOG_TAG, "Quảng cáo tải thất bại :$p0 ")
                }
            })
    }
    fun showAdIfAvailable(activity: Activity){

        if(!isAdAvailable() || isShowingAd){
            Log.d(LOG_TAG, "Quảng cáo không khả dụng hoặc đang hiển thị")
            return
        }
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback(){
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                appOpenAd = null
                isShowingAd = false
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
            }
        }
        Log.d(LOG_TAG, "Hiển thị quảng cáo")
        appOpenAd?.show(activity)?:Log.d(LOG_TAG, "appOpenAd null")
    }
    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

}