package com.meubebe.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class MeuBebeApp : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAdManager: AppOpenAdManager? = null
    private var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)

        // Inicializa o AdMob em background
        MobileAds.initialize(this) {}

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        appOpenAdManager = AppOpenAdManager()
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Mostra o anúncio quando o app volta para o primeiro plano
        currentActivity?.let { activity ->
            appOpenAdManager?.showAdIfAvailable(activity)
        }
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    /**
     * Gerenciador interno do App Open Ad
     */
    private inner class AppOpenAdManager {
        private val adUnitId = "ca-app-pub-3016178863457047/8931527209"
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        private var isShowingAd = false
        private var loadTime: Long = 0

        fun loadAd() {
            if (isLoadingAd || isAdAvailable()) return

            isLoadingAd = true
            val request = AdRequest.Builder().build()
            AppOpenAd.load(
                this@MeuBebeApp,
                adUnitId,
                request,
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        isLoadingAd = false
                        loadTime = Date().time
                        // Se for a primeira inicialização com tela ativa, exibe de imediato
                        currentActivity?.let { showAdIfAvailable(it) }
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        isLoadingAd = false
                        appOpenAd = null
                    }
                }
            )
        }

        private fun wasLoadTimeLessThan4HoursAgo(): Boolean {
            val numHours = 4
            val dateDifference: Long = Date().time - loadTime
            val numMilliSecondsPerHour: Long = 3600000
            return dateDifference < numMilliSecondsPerHour * numHours
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null && wasLoadTimeLessThan4HoursAgo()
        }

        fun showAdIfAvailable(activity: Activity) {
            if (isShowingAd) return

            if (!isAdAvailable()) {
                loadAd()
                return
            }

            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    isShowingAd = false
                    loadAd() // Prepara o próximo anúncio
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    isShowingAd = false
                    loadAd()
                }

                override fun onAdShowedFullScreenContent() {
                    isShowingAd = true
                }
            }

            isShowingAd = true
            appOpenAd?.show(activity)
        }
    }
}
