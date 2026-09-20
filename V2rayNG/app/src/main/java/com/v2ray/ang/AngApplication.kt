package com.v2ray.ang

import android.content.Context
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.WorkManager
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig.ANG_PACKAGE
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.SettingsManager

class AngApplication : MultiDexApplication() {
    companion object {
        lateinit var application: AngApplication
        private const val CLUB_SUBSCRIPTION_REMARKS = "club"
    }

    /**
     * Attaches the base context to the application.
     * @param base The base context.
     */
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        application = this
    }

    private val workManagerConfiguration: Configuration = Configuration.Builder()
        .setDefaultProcessName("${ANG_PACKAGE}:bg")
        .build()

    /**
     * Initializes the application.
     */
    override fun onCreate() {
        super.onCreate()

        MMKV.initialize(this)

        // Initialize WorkManager with the custom configuration
        WorkManager.initialize(this, workManagerConfiguration)

        // Ensure critical preference defaults are present in MMKV early
        SettingsManager.initApp(this)
        SettingsManager.setNightMode()

        ensureClubVpnSubscription()

        es.dmoral.toasty.Toasty.Config.getInstance()
            .setGravity(android.view.Gravity.BOTTOM, 0, 300)
            .apply()
    }

    /**
     * Provision the subscription embedded in this ClubVPN build exactly once.
     * The release pipeline replaces the resource value before signing the APK.
     */
    private fun ensureClubVpnSubscription() {
        if (MmkvManager.decodeSubscriptions().any { it.subscription.remarks == CLUB_SUBSCRIPTION_REMARKS }) {
            return
        }

        MmkvManager.encodeSubscription(
            "",
            SubscriptionItem(
                remarks = CLUB_SUBSCRIPTION_REMARKS,
                url = getString(R.string.club_subscription_url),
                enabled = true,
                autoUpdate = true,
            ),
        )
    }
}
