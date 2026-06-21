package com.hiskytechs.muhallinewuserapp

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import com.hiskytechs.muhallinewuserapp.network.AppSession
import com.hiskytechs.muhallinewuserapp.network.CurrencyFormatter
import com.hiskytechs.muhallinewuserapp.Utill.ThemeManager

class MuhalliApplication : Application() {
    companion object {
        lateinit var instance: MuhalliApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeManager.applySavedTheme(this)
        LocaleManager.applySavedLocale(this)
        AppSession.initialize(this)
        CurrencyFormatter.initialize(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window?.decorView?.post {
                    LocaleManager.applyLayoutDirection(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) = Unit

            override fun onActivityResumed(activity: Activity) {
                LocaleManager.applyLayoutDirection(activity)
            }

            override fun onActivityPaused(activity: Activity) = Unit

            override fun onActivityStopped(activity: Activity) = Unit

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }
}
