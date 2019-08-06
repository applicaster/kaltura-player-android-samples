package com.kaltura.playkitdemo;

import android.app.Application;
import android.content.res.Configuration;

import com.kaltura.playkit.player.PKHttpClientManager;
import com.kaltura.tvplayer.KalturaPlayer;

import static com.kaltura.playkitdemo.PartnersConfig.OTT_PARTNER_ID;
import static com.kaltura.playkitdemo.PartnersConfig.OTT_SERVER_URL;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_PARTNER_ID;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_PARTNER_ID_CLEAR;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_PARTNER_ID_DRM;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_PARTNER_ID_HLS;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_PARTNER_ID_LIVE;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_PARTNER_ID_LIVE_1;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_PARTNER_ID_VR;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_SERVER_URL;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_SERVER_URL_CLEAR;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_SERVER_URL_DRM;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_SERVER_URL_HLS;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_SERVER_URL_LIVE;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_SERVER_URL_LIVE_1;
import static com.kaltura.playkitdemo.PartnersConfig.OVP_SERVER_URL_VR;


public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        KalturaPlayer.initializeOTT(this, OTT_PARTNER_ID, OTT_SERVER_URL);
        KalturaPlayer.initializeOVP(this, OVP_PARTNER_ID, OVP_SERVER_URL);
        KalturaPlayer.initializeOVP(this, OVP_PARTNER_ID_HLS, OVP_SERVER_URL_HLS);
        KalturaPlayer.initializeOVP(this, OVP_PARTNER_ID_DRM, OVP_SERVER_URL_DRM);
        KalturaPlayer.initializeOVP(this, OVP_PARTNER_ID_VR, OVP_SERVER_URL_VR);
        KalturaPlayer.initializeOVP(this, OVP_PARTNER_ID_CLEAR, OVP_SERVER_URL_CLEAR);
        KalturaPlayer.initializeOVP(this, OVP_PARTNER_ID_LIVE, OVP_SERVER_URL_LIVE);
        KalturaPlayer.initializeOVP(this, OVP_PARTNER_ID_LIVE_1, OVP_SERVER_URL_LIVE_1);
        //doConnectionsWarmup();
    }

    private void doConnectionsWarmup() {
        PKHttpClientManager.setHttpProvider("okhttp");
        PKHttpClientManager.warmUp(
                "https://rest-as.ott.kaltura.com/crossdomain.xml",
                "https://api-preprod.ott.kaltura.com/crossdomain.xml",
                "https://cdnapisec.kaltura.com/favicon.ico",
                "https://cfvod.kaltura.com/favicon.ico"
        );
    }
    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}