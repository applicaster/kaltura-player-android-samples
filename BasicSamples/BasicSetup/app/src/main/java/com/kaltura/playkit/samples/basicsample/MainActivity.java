package com.kaltura.playkit.samples.basicsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import com.kaltura.playkit.PKDrmParams;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaFormat;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.tvplayer.KalturaPlayer;
import com.kaltura.tvplayer.PlayerInitOptions;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final Long START_POSITION = 0L; // position tp start playback in msec.

    private static final PKMediaFormat MEDIA_FORMAT = PKMediaFormat.hls;
    private static final String SOURCE_URL = "https://cdnapisec.kaltura.com/p/2215841/sp/221584100/playManifest/entryId/1_w9zx2eti/protocol/https/format/applehttp/falvorIds/1_1obpcggb,1_yyuvftfz,1_1xdbzoa6,1_k16ccgto,1_djdf6bk8/a.m3u8";
    private static final String LICENSE_URL = null;

    private KalturaPlayer player;
    private Button playPauseButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PKMediaEntry mediaEntry = createMediaEntry();

        loadPlaykitPlayer(mediaEntry);

        addPlayPauseButton();

    }

    /**
     * Create {@link PKMediaEntry} with minimum necessary data.
     *
     * @return - the {@link PKMediaEntry} object.
     */
    private PKMediaEntry createMediaEntry() {
        //Create media entry.
        PKMediaEntry mediaEntry = new PKMediaEntry();

        //Set id for the entry.
        mediaEntry.setId("testEntry");

        //Set media entry type. It could be Live,Vod or Unknown.
        //In this sample we use Vod.
        mediaEntry.setMediaType(PKMediaEntry.MediaEntryType.Vod);

        //Create list that contains at least 1 media source.
        //Each media entry can contain a couple of different media sources.
        //All of them represent the same content, the difference is in it format.
        //For example same entry can contain PKMediaSource with dash and another
        // PKMediaSource can be with hls. The player will decide by itself which source is
        // preferred for playback.
        List<PKMediaSource> mediaSources = createMediaSources();

        //Set media sources to the entry.
        mediaEntry.setSources(mediaSources);

        return mediaEntry;
    }

    /**
     * Create list of {@link PKMediaSource}.
     *
     * @return - the list of sources.
     */
    private List<PKMediaSource> createMediaSources() {

        //Create new PKMediaSource instance.
        PKMediaSource mediaSource = new PKMediaSource();

        //Set the id.
        mediaSource.setId("testSource");

        //Set the content url. In our case it will be link to hls source(.m3u8).
        mediaSource.setUrl(SOURCE_URL);

        //Set the format of the source. In our case it will be hls in case of mpd/wvm formats you have to to call mediaSource.setDrmData method as well
        mediaSource.setMediaFormat(MEDIA_FORMAT);

        // Add DRM data if required
        if (LICENSE_URL != null) {
            mediaSource.setDrmData(Collections.singletonList(
                    new PKDrmParams(LICENSE_URL, PKDrmParams.Scheme.WidevineCENC)
            ));
        }

        return Collections.singletonList(mediaSource);
    }

    /**
     * Just add a simple button which will start/pause playback.
     */
    private void addPlayPauseButton() {
        //Get reference to the play/pause button.
        playPauseButton = this.findViewById(R.id.play_pause_button);
        //Add clickListener.
        playPauseButton.setOnClickListener(v -> {
            if (player.isPlaying()) {
                //If player is playing, change text of the button and pause.
                playPauseButton.setText(R.string.play_text);
                player.pause();
            } else {
                //If player is not playing, change text of the button and play.
                playPauseButton.setText(R.string.pause_text);
                player.play();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.onApplicationResumed();
            player.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.onApplicationPaused();
        }
    }

    public void loadPlaykitPlayer(PKMediaEntry pkMediaEntry) {
        PlayerInitOptions playerInitOptions = new PlayerInitOptions();
//        updatedInitOptions.setLicenseRequestAdapter(initOptions.licenseRequestAdapter);
//        updatedInitOptions.setContentRequestAdapter(initOptions.contentRequestAdapter);
//        updatedInitOptions.setVrPlayerEnabled(initOptions.vrPlayerEnabled);
//        updatedInitOptions.setVRSettings(initOptions.vrSettings);
//        updatedInitOptions.setAdAutoPlayOnResume(initOptions.adAutoPlayOnResume);
//        updatedInitOptions.setSubtitleStyle(initOptions.setSubtitleStyle);
//        updatedInitOptions.setLoadControlBuffers(initOptions.loadControlBuffers);
//        updatedInitOptions.setAbrSettings(initOptions.abrSettings);
//        updatedInitOptions.setAspectRatioResizeMode(initOptions.aspectRatioResizeMode);
//        updatedInitOptions.setPreferredMediaFormat(initOptions.preferredMediaFormat != null ?initOptions.preferredMediaFormat.name() : null);
//        updatedInitOptions.setAllowClearLead(initOptions.allowClearLead);
//        updatedInitOptions.setAllowCrossProtocolEnabled(initOptions.allowCrossProtocolEnabled);
//        updatedInitOptions.setSecureSurface(initOptions.secureSurface);
//        updatedInitOptions.setKs(initOptions.ks);
//        updatedInitOptions.setServerUrl("https://cdnapisec.kaltura.com/");
//        updatedInitOptions.setAutoPlay(initOptions.autoplay);
//        updatedInitOptions.setReferrer(initOptions.referrer);
//        if (initOptions.audioLanguage != null && initOptions.audioLanguageMode != null) {
//            updatedInitOptions.setAudioLanguage(initOptions.audioLanguage, initOptions.audioLanguageMode);
//        }
//        if (initOptions.textLanguage != null && initOptions.textLanguageMode != null) {
//            updatedInitOptions.setTextLanguage(initOptions.textLanguage, initOptions.textLanguageMode);
//        }

        player = KalturaPlayer.createBasicPlayer(MainActivity.this, playerInitOptions);
        player.setMedia(pkMediaEntry, START_POSITION);
        player.setPlayerView(FrameLayout.LayoutParams.WRAP_CONTENT, 600);

        ViewGroup container = findViewById(R.id.player_root);
        container.addView(player.getPlayerView());
    }

}
