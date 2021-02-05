package com.github.treehollow.ui

import android.content.pm.PackageInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.treehollow.R
import com.github.treehollow.base.TreeHollowApplication
import com.github.treehollow.ui.login.WelcomeActivity
import com.github.treehollow.ui.mainscreen.MainScreenActivity
import com.github.treehollow.utils.Utils
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.distribute.Distribute
import com.skydoves.bundler.intentOf
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setActivityTheme(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCenter.start(
            application, "70173f41-00a6-4236-9fb6-6199c70d2414",
            Analytics::class.java, Crashes::class.java
        )
        Distribute.disableAutomaticCheckForUpdate()

        val pInfo: PackageInfo =
            TreeHollowApplication.instance.packageManager.getPackageInfo(
                TreeHollowApplication.instance.packageName, 0
            )
        val version = pInfo.versionName
        val properties: MutableMap<String, String> = HashMap()
        properties["version"] = version
        Analytics.trackEvent("StartVersion")

        lifecycleScope.launch {
            val result = TreeHollowApplication.token
            if (result != null) {
                intentOf<MainScreenActivity> {
                    startActivity(this@MainActivity)
                }
                finish()

            } else {
                intentOf<WelcomeActivity> {
                    startActivity(this@MainActivity)
                }
                finish()
            }
        }
    }
}