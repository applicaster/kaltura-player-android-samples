package com.kaltura.playkit.samples.subtitlesideloading

import android.graphics.Color
import android.os.Bundle
import android.text.Layout
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kaltura.playkit.PKRequestConfig
import com.kaltura.playkit.PKSubtitleFormat
import com.kaltura.playkit.PlayerEvent
import com.kaltura.playkit.PlayerState
import com.kaltura.playkit.ads.AdController
import com.kaltura.playkit.player.*
import com.kaltura.playkit.providers.api.phoenix.APIDefines
import com.kaltura.playkit.providers.ott.OTTMediaAsset
import com.kaltura.playkit.providers.ott.PhoenixMediaProvider
import com.kaltura.playkit.samples.subtitlesideloading.tracks.TrackItem
import com.kaltura.playkit.samples.subtitlesideloading.tracks.TrackItemAdapter
import com.kaltura.tvplayer.KalturaOttPlayer
import com.kaltura.tvplayer.KalturaPlayer
import com.kaltura.tvplayer.OTTMediaOptions
import com.kaltura.tvplayer.PlayerInitOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.tracks_selection_menu_layout.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private val TAG = "MainActivity"
    private val ASSET_ID = "548576"
    private val START_POSITION = 0L // position for start playback in msec.
    private var player: KalturaPlayer? = null

    private var userIsInteracting: Boolean = false
    private var isFullScreen: Boolean = false
    private var playerState: PlayerState? = null

    /**
     * Get the external subtitles list
     */

    private val externalSubtitles: List<PKExternalSubtitle>
        get() {

            val mList = ArrayList<PKExternalSubtitle>()

            val pkExternalSubtitle = PKExternalSubtitle()
                    .setUrl("http://brenopolanski.com/html5-video-webvtt-example/MIB2-subtitles-pt-BR.vtt")
                    .setMimeType(PKSubtitleFormat.vtt)
                    .setLabel("External_Deutsch")
                    .setLanguage("deu")
            mList.add(pkExternalSubtitle)

            val pkExternalSubtitleDe = PKExternalSubtitle()
                    .setUrl("https://mkvtoolnix.download/samples/vsshort-en.srt")
                    .setMimeType(PKSubtitleFormat.srt)
                    .setLabel("External_English")
                    .setLanguage("eng")
                    .setDefault()
            mList.add(pkExternalSubtitleDe)

            return mList
        }

    private val defaultPositionDefault: SubtitleStyleSettings
        get() = SubtitleStyleSettings("DefaultStyle")
                // Set the subtitle position to default. Need to set the other apis to change the values
                .setSubtitlePosition(PKSubtitlePosition(true))

    private val styleForPositionOne: SubtitleStyleSettings
        get() = SubtitleStyleSettings("KidsStyle")
                .setBackgroundColor(Color.BLUE)
                .setTextColor(Color.WHITE)
                .setTextSizeFraction(SubtitleStyleSettings.SubtitleTextSizeFraction.SUBTITLE_FRACTION_50)
                .setWindowColor(Color.YELLOW)
                .setEdgeColor(Color.BLUE)
                .setTypeface(SubtitleStyleSettings.SubtitleStyleTypeface.MONOSPACE)
                .setEdgeType(SubtitleStyleSettings.SubtitleStyleEdgeType.EDGE_TYPE_DROP_SHADOW)
                // Move subtitle horizontal and vertical together (anywhere on the screen)
                .setSubtitlePosition(PKSubtitlePosition(true).setPosition( 50, 50, Layout.Alignment. ALIGN_NORMAL))

    private val styleForPositionTwo: SubtitleStyleSettings
        get() = SubtitleStyleSettings("AdultsStyle")
                .setBackgroundColor(Color.WHITE)
                .setTextColor(Color.BLUE)
                .setTextSizeFraction(SubtitleStyleSettings.SubtitleTextSizeFraction.SUBTITLE_FRACTION_100)
                .setWindowColor(Color.BLUE)
                .setEdgeColor(Color.BLUE)
                .setTypeface(SubtitleStyleSettings.SubtitleStyleTypeface.SANS_SERIF)
                .setEdgeType(SubtitleStyleSettings.SubtitleStyleEdgeType.EDGE_TYPE_DROP_SHADOW)
                // Move subtitle vertical up-down
                .setSubtitlePosition(PKSubtitlePosition(true).setVerticalPosition(30))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadPlaykitPlayer()

        activity_main.setOnClickListener { v ->
            if (isFullScreen) {
                tracks_selection_menu.animate().translationY(0f)
                isFullScreen = false
            } else {
                tracks_selection_menu.animate().translationY(-200f)
                isFullScreen = true
            }
        }

    }

    /**
     * Just add a simple button which will start/pause playback.
     */
    private fun addPlayPauseButton() {
        //Add clickListener.
        play_pause_button.setOnClickListener { v ->
            player?.let {
                val adController = it.getController(AdController::class.java)
                if (it.isPlaying || adController != null && adController.isAdDisplayed && adController.isAdPlaying) {
                    if (adController != null && adController.isAdDisplayed) {
                        adController.pause()
                    } else {
                        it.pause()
                    }
                    //If player is playing, change text of the button and pause.
                    play_pause_button.setText(R.string.play_text)
                } else {
                    if (adController != null && adController.isAdDisplayed) {
                        adController.play()
                    } else {
                        it.play()
                    }
                    //If player is not playing, change text of the button and play.
                    play_pause_button.setText(R.string.pause_text)

                }
            }
        }
    }

    /**
     * Here we are getting access to the Android Spinner views,
     * and set OnItemSelectedListener.
     */
    private fun initializeTrackSpinners() {
        tvSpinnerTitle.visibility = View.INVISIBLE
        ccStyleSpinner.visibility = View.INVISIBLE

        textSpinner.onItemSelectedListener = this
        audioSpinner.onItemSelectedListener = this
        videoSpinner.onItemSelectedListener = this

        val stylesStrings = ArrayList<String>()
        stylesStrings.add(defaultPositionDefault.styleName)
        stylesStrings.add(styleForPositionOne.styleName)
        stylesStrings.add(styleForPositionTwo.styleName)
        val ccStyleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stylesStrings)
        ccStyleSpinner.adapter = ccStyleAdapter
        ccStyleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (!userIsInteracting) {
                    return
                }

                if (position == 0) {
                    player?.updateSubtitleStyle(defaultPositionDefault)
                } else if (position == 1) {
                    player?.updateSubtitleStyle(styleForPositionOne)
                } else {
                    player?.updateSubtitleStyle(styleForPositionTwo)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
    }

    /**
     * Subscribe to the TRACKS_AVAILABLE event. This event will be sent
     * every time new source have been loaded and it tracks data is obtained
     * by the player.
     */
    private fun subscribeToTracksAvailableEvent() {
        player?.addListener(this, PlayerEvent.tracksAvailable) { event ->
            //When the track data available, this event occurs. It brings the info object with it.
            Log.d(TAG, "Event TRACKS_AVAILABLE")

            //Obtain the actual tracks info from it. Default track index values are coming from manifest
            val tracks = event.tracksInfo
            val defaultAudioTrackIndex = tracks.getDefaultAudioTrackIndex()
            val defaultTextTrackIndex = tracks.getDefaultTextTrackIndex()
            if (tracks.audioTracks.size > 0) {
                Log.d(TAG, "Default Audio language = " + tracks.audioTracks[defaultAudioTrackIndex].label)
            }
            if (tracks.textTracks.size > 0) {
                Log.d(TAG, "Default Text language = " + tracks.textTracks[defaultTextTrackIndex].label)
                tvSpinnerTitle.visibility = View.VISIBLE
                ccStyleSpinner.visibility = View.VISIBLE

            }
            if (tracks.getVideoTracks().size > 0) {
                Log.d(TAG, "Default video isAdaptive = " + tracks.videoTracks[tracks.defaultAudioTrackIndex].isAdaptive + " bitrate = " + tracks.videoTracks[tracks.defaultAudioTrackIndex].bitrate)
            }
            //player.changeTrack(tracksAvailable.tracksInfo.getVideoTracks().get(1).getUniqueId());
            //Populate Android spinner views with received data.
            populateSpinnersWithTrackInfo(tracks)
        }

        player?.addListener(this, PlayerEvent.videoTrackChanged) { event -> Log.d(TAG, "Event VideoTrackChanged ${event.newTrack.getBitrate()}") }

        player?.addListener(this, PlayerEvent.audioTrackChanged) { event -> Log.d(TAG, "Event AudioTrackChanged ${event.newTrack.getLanguage()}") }

        player?.addListener(this, PlayerEvent.textTrackChanged) { event -> Log.d(TAG, "Event TextTrackChanged ${event.newTrack.getLanguage()}")}

        player?.addListener(this, PlayerEvent.subtitlesStyleChanged) { event -> Log.d(TAG, "Event SubtitlesStyleChanged " + event.styleName) }

        player?.addListener(this, PlayerEvent.stateChanged) { event ->
            Log.d(TAG, "State changed from " + event.oldState + " to " + event.newState)
            playerState = event.newState
        }
    }

    /**
     * Populate Android Spinners with retrieved tracks data.
     * Here we are building custom [TrackItem] objects, with which
     * we will populate our custom spinner adapter.
     * @param tracks - [PKTracks] object with all tracks data in it.
     */
    private fun populateSpinnersWithTrackInfo(tracks: PKTracks) {

        //Build track items that are based on videoTrack data.
        val videoTrackItems = buildVideoTrackItems(tracks.videoTracks)
        //populate spinner with this info.
        applyAdapterOnSpinner(videoSpinner, videoTrackItems, tracks.defaultVideoTrackIndex)

        //Build track items that are based on audioTrack data.
        val audioTrackItems = buildAudioTrackItems(tracks.audioTracks)
        //populate spinner with this info.
        applyAdapterOnSpinner(audioSpinner, audioTrackItems, tracks.defaultAudioTrackIndex)

        //Build track items that are based on textTrack data.
        val textTrackItems = buildTextTrackItems(tracks.textTracks)
        //populate spinner with this info.
        applyAdapterOnSpinner(textSpinner, textTrackItems, tracks.defaultTextTrackIndex)
    }

    /**
     * Will build array of [TrackItem] objects.
     * Each [TrackItem] object will hold the readable name.
     * In this case the width and height of the video track.
     * If [VideoTrack] is adaptive, we will name it "Auto".
     * We use this name to represent the track selection options.
     * Also each [TrackItem] will hold the unique id of the track,
     * which should be passed to the player in order to switch to the desired track.
     * @param videoTracks - the list of available video tracks.
     * @return - array with custom [TrackItem] objects.
     */
    private fun buildVideoTrackItems(videoTracks: List<VideoTrack>): Array<TrackItem?> {
        //Initialize TrackItem array with size of videoTracks list.
        val trackItems = arrayOfNulls<TrackItem>(videoTracks.size)

        //Iterate through all available video tracks.
        for (i in videoTracks.indices) {
            //Get video track from index i.
            val videoTrackInfo = videoTracks[i]

            //Check if video track is adaptive. If so, give it "Auto" name.
            if (videoTrackInfo.isAdaptive) {
                //In this case, if this track is selected, the player will
                //adapt the playback bitrate automatically, based on user bandwidth and device capabilities.
                //Initialize TrackItem.
                trackItems[i] = TrackItem("Auto", videoTrackInfo.uniqueId)
            } else {

                //If it is not adaptive track, build readable name based on width and height of the track.
                val nameStringBuilder = StringBuilder()
                nameStringBuilder.append(videoTrackInfo.bitrate)

                //Initialize TrackItem.
                trackItems[i] = TrackItem(nameStringBuilder.toString(), videoTrackInfo.uniqueId)
            }
        }
        return trackItems
    }

    /**
     * Will build array of [TrackItem] objects.
     * Each [TrackItem] object will hold the readable name.
     * In this case the label of the audio track.
     * If [AudioTrack] is adaptive, we will name it "Auto".
     * We use this name to represent the track selection options.
     * Also each [TrackItem] will hold the unique id of the track,
     * which should be passed to the player in order to switch to the desired track.
     * @param audioTracks - the list of available audio tracks.
     * @return - array with custom [TrackItem] objects.
     */
    private fun buildAudioTrackItems(audioTracks: List<AudioTrack>): Array<TrackItem?> {
        //Initialize TrackItem array with size of audioTracks list.
        val trackItems = arrayOfNulls<TrackItem>(audioTracks.size)

        val channelMap = HashMap<Int, AtomicInteger>()
        for (i in audioTracks.indices) {
            if (channelMap.containsKey(audioTracks[i].channelCount)) {
                channelMap[audioTracks[i].channelCount]?.incrementAndGet()
            } else {
                channelMap[audioTracks[i].channelCount] = AtomicInteger(1)
            }
        }
        var addChannel = false

        if (channelMap.keys.size > 0 && AtomicInteger(audioTracks.size).toString() != channelMap[audioTracks[0].channelCount].toString()) {
            addChannel = true
        }


        //Iterate through all available audio tracks.
        for (i in audioTracks.indices) {
            val audioTrackInfo = audioTracks[i]
            var label: String? = ""
            if (audioTrackInfo != null) {
                label = if (audioTrackInfo.label != null) audioTrackInfo.label else if (audioTrackInfo.language != null) audioTrackInfo.language else ""
            }
            var bitrate = if (audioTrackInfo.bitrate > 0) "" + audioTrackInfo.bitrate else ""
            if (TextUtils.isEmpty(bitrate) && addChannel) {
                bitrate = buildAudioChannelString(audioTrackInfo.channelCount)
            }
            if (audioTrackInfo.isAdaptive) {
                bitrate += " Adaptive"
            }
            trackItems[i] = TrackItem("$label $bitrate", audioTrackInfo.uniqueId)
        }
        return trackItems
    }


    private fun buildAudioChannelString(channelCount: Int): String {
        when (channelCount) {
            1 -> return "Mono"
            2 -> return "Stereo"
            6, 7 -> return "Surround_5.1"
            8 -> return "Surround_7.1"
            else -> return "Surround"
        }
    }


    /**
     * Will build array of [TrackItem] objects.
     * Each [TrackItem] object will hold the readable name.
     * In this case the label of the text track.
     * We use this name to represent the track selection options.
     * Also each [TrackItem] will hold the unique id of the track,
     * which should be passed to the player in order to switch to the desired track.
     * @param textTracks - the list of available text tracks.
     * @return - array with custom [TrackItem] objects.
     */
    private fun buildTextTrackItems(textTracks: List<TextTrack>): Array<TrackItem?> {
        //Initialize TrackItem array with size of textTracks list.
        val trackItems = arrayOfNulls<TrackItem>(textTracks.size)

        //Iterate through all available text tracks.
        for (i in textTracks.indices) {

            //Get text track from index i.
            val textTrackInfo = textTracks[i]

            //Name TrackItem based on the text track label.
            val name = textTrackInfo.label
            trackItems[i] = TrackItem(name, textTrackInfo.uniqueId)
        }
        return trackItems
    }

    /**
     * Initialize and set custom adapter to the Android spinner.
     * @param spinner - spinner to which adapter should be applied.
     * @param trackItems - custom track items array.
     */
    private fun applyAdapterOnSpinner(spinner: Spinner, trackItems: Array<TrackItem?>, defaultSelectedIndex: Int) {
        //Initialize custom adapter.
        val trackItemAdapter = TrackItemAdapter(this, R.layout.track_items_list_row, trackItems)
        //Apply adapter on spinner.
        spinner.adapter = trackItemAdapter

        if (defaultSelectedIndex > 0) {
            spinner.setSelection(defaultSelectedIndex)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (!userIsInteracting) {
            return
        }
        //Get the selected TrackItem from adapter.
        val trackItem = parent.getItemAtPosition(position) as TrackItem

        //Important! This will actually do the switch between tracks.
        player?.changeTrack(trackItem.uniqueId)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {

    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        userIsInteracting = true
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        player?.let {
            play_pause_button.setText(R.string.pause_text)
            player?.onApplicationPaused()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.destroy();
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()

        player?.let {
            if (it.mediaEntry != null) {
                it.onApplicationResumed()
                it.play()
            }
        }
    }

    fun loadPlaykitPlayer() {

        val playerInitOptions = PlayerInitOptions(PARTNER_ID)
        playerInitOptions.setSubtitleStyle(defaultPositionDefault)
        playerInitOptions.setPKRequestConfig(PKRequestConfig(true))
        playerInitOptions.setAutoPlay(true)


        /*
                  //PhoenixTVPlayerParams
                  "analyticsUrl": "https://analytics.kaltura.com/api_v3/index.php"
                  "ovpServiceUrl": "https://cdnapisec.kaltura.com"
                  "ovpPartnerId": 2254732
                  "uiConfId": 44267972
                 */


        player = KalturaOttPlayer.create(this@MainActivity, playerInitOptions)
        player?.setPlayerView(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        val container = player_root
        container.addView(player?.playerView)

        val ottMediaOptions = buildOttMediaOptions()
        player?.loadMedia(ottMediaOptions) { mediaOptions, entry, loadError ->
            if (loadError != null) {
                Snackbar.make(findViewById(android.R.id.content), loadError.message, Snackbar.LENGTH_LONG).show()
            } else {
                Log.d(TAG, "OTTMedia onEntryLoadComplete  entry = " + entry.id)
            }
        }

        //Add simple play/pause button.
        addPlayPauseButton()

        //Initialize Android spinners view.
        initializeTrackSpinners()

        //Subscribe to the event which will notify us when track data is available.
        subscribeToTracksAvailableEvent()
    }

    private fun buildOttMediaOptions(): OTTMediaOptions {
        val ottMediaAsset = OTTMediaAsset()
        ottMediaAsset.assetId = ASSET_ID
        ottMediaAsset.assetType = APIDefines.KalturaAssetType.Media
        ottMediaAsset.contextType = APIDefines.PlaybackContextType.Playback
        ottMediaAsset.assetReferenceType = APIDefines.AssetReferenceType.Media
        ottMediaAsset.protocol = PhoenixMediaProvider.HttpProtocol.Http
        ottMediaAsset.formats = listOf("Mobile_Main")
        ottMediaAsset.ks = null

        val ottMediaOptions = OTTMediaOptions(ottMediaAsset)
        ottMediaOptions.startPosition = START_POSITION
        ottMediaOptions.externalSubtitles = externalSubtitles
        //ottMediaOptions.externalVttThumbnailUrl = "https://stdlwcdn.lwcdn.com/i/8fdb4e20-8ebb-4590-8844-dae39680d837/160p.vtt"

        return ottMediaOptions
    }

    companion object {
        val SERVER_URL = "https://rest-us.ott.kaltura.com/v4_5/api_v3/"
        val PARTNER_ID = 3009
    }
}
