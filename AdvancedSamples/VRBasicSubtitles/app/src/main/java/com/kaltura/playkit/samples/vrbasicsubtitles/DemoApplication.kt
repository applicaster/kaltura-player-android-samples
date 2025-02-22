package com.kaltura.playkit.samples.vrbasicsubtitles

import android.content.res.Configuration
import androidx.multidex.MultiDexApplication

import com.kaltura.playkit.player.PKHttpClientManager
import com.kaltura.tvplayer.KalturaOvpPlayer


class DemoApplication: MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        KalturaOvpPlayer.initialize(this, MainActivity.PARTNER_ID, MainActivity.SERVER_URL)
        doConnectionsWarmup()
    }

    private fun doConnectionsWarmup() {
        PKHttpClientManager.setHttpProvider("okhttp")
        PKHttpClientManager.warmUp(
                "https://rest-us.ott.kaltura.com/crossdomain.xml",
                "http://cdnapi.kaltura.com/alive.html",
                "https://cdnapisec.kaltura.com/favicon.ico",
                "https://cfvod.kaltura.com/favicon.ico"
        )
    }

    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    override fun onLowMemory() {
        super.onLowMemory()
    }
}