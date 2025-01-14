package com.kaltura.playlistbyid

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.PlayerEvent
import com.kaltura.playkit.PlayerState
import com.kaltura.playkit.plugins.ads.AdEvent
import com.kaltura.tvplayer.KalturaOvpPlayer
import com.kaltura.tvplayer.KalturaPlayer
import com.kaltura.tvplayer.PlayerInitOptions
import com.kaltura.tvplayer.playlist.OVPPlaylistIdOptions
import com.kaltura.tvplayer.playlist.PlaylistEvent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        val PARTNER_ID = 2068231
        val SERVER_URL = "https://cdnapisec.kaltura.com"
    }

    private val log = PKLog.get("MainActivity")
    private val START_POSITION = 0L // position for start playback in msec.
    private var player: KalturaPlayer? = null

    private val ENTRY_ID_BY_ID = "1_j9v8qs8h"
    private var isFullScreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadKalturaPlaylistById()

        showSystemUI()

        activity_main.setOnClickListener { v ->
            if (isFullScreen) {
                showSystemUI()
            } else {
                hideSystemUI()
            }
        }

        btn_shuffle.visibility = View.GONE
//        btn_shuffle.setOnClickListener {
//            player?.let {
//                it.playlistController.shuffle(!it.playlistController.isShuffleEnabled)
//                btn_shuffle.text = "Shuffle : ${it.playlistController.isShuffleEnabled}"
//            }
//        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    or View.SYSTEM_UI_FLAG_IMMERSIVE)
        }
        isFullScreen = true
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        isFullScreen = false
    }

    @SuppressLint("SetTextI18n")
    private fun addPlayerListeners() {
        player?.addListener(this, PlaylistEvent.playListLoaded) { event ->
            log.d("PLAYLIST playListLoaded")
            btn_shuffle.visibility = View.INVISIBLE
            //btn_shuffle.text = "Shuffle : ${player?.playlistController?.isShuffleEnabled}"
        }

        player?.addListener(this, PlaylistEvent.playListStarted) { event ->
            log.d("PLAYLIST playListStarted")
        }

//        player?.addListener(this, PlaylistEvent.playlistShuffleStateChanged) { event ->
//            log.d("PLAYLIST playlistShuffleStateChanged ${event.mode}")
//        }

        player?.addListener(this, PlaylistEvent.playlistLoopStateChanged) { event ->
            log.d("PLAYLIST playlistLoopStateChanged ${event.mode}")
        }

        player?.addListener(this, PlaylistEvent.playlistAutoContinueStateChanged) { event ->
            log.d("PLAYLIST playlistLoopStateChanged ${event.mode}")
        }

        player?.addListener(this, PlaylistEvent.playListEnded) { event ->
            log.d("PLAYLIST playListEnded")
        }

        player?.addListener(this, PlaylistEvent.playListError) { event ->
            log.d("PLAYLIST playListError")
            Toast.makeText(this, event.error.message, Toast.LENGTH_SHORT).show()
        }

        player?.addListener(this, PlaylistEvent.playListLoadMediaError) { event ->
            log.d("PLAYLIST PlaylistLoadMediaError")
            Toast.makeText(this, event.error.message, Toast.LENGTH_SHORT).show()
        }

        player?.addListener(this, PlaylistEvent.playlistCountDownStart) { event ->
            log.d("playlistCountDownStart currentPlayingIndex = " + event.currentPlayingIndex + " durationMS = " + event.playlistCountDownOptions?.durationMS);
        }

        player?.addListener(this, PlaylistEvent.playlistCountDownEnd) { event ->
            log.d("playlistCountDownEnd currentPlayingIndex = " + event.currentPlayingIndex + " durationMS = " + event.playlistCountDownOptions?.durationMS);
        }

        player?.addListener(this, PlayerEvent.stateChanged) { event ->
            log.d("State changed from ${event.oldState} to ${event.newState}")
            playerControls.setPlayerState(event.newState)
        }

        player?.addListener(this, AdEvent.contentResumeRequested) { event ->
            log.d("CONTENT_RESUME_REQUESTED")
            playerControls.setPlayerState(PlayerState.READY)
        }
    }

    override fun onResume() {
        super.onResume()
        player?.let {
            if (it.mediaEntry != null) {
                it.onApplicationResumed()
                it.play()
            }
        }

        playerControls.resume();
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.destroy();
    }

    override fun onPause() {
        super.onPause()
        player?.onApplicationPaused()
        playerControls.release()
    }

    /**
     * To load a playlist by ID, use [OVPPlaylistIdOptions] when calling loadPlaylistById method.
     */
    private fun loadKalturaPlaylistById() {
        val playerInitOptions = PlayerInitOptions(PARTNER_ID)
        playerInitOptions.setAutoPlay(true)
        playerInitOptions.setReferrer("app://testing.app.com")
        player = KalturaOvpPlayer.create(this@MainActivity, playerInitOptions)

        player?.setPlayerView(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        val container = player_root
        container.addView(player?.playerView)

        playerControls.setPlayer(player)

        val ovpPlaylistIdOptions = OVPPlaylistIdOptions()
        ovpPlaylistIdOptions.playlistId = ENTRY_ID_BY_ID
        ovpPlaylistIdOptions.loopEnabled = true
        //ovpPlaylistIdOptions.shuffleEnabled = false

        player?.loadPlaylistById(ovpPlaylistIdOptions) { _, error ->
            if (error != null) {
                Snackbar.make(findViewById(android.R.id.content), error.message, Snackbar.LENGTH_LONG).show()
            } else {
                log.d("OVPPlaylist OnPlaylistLoadListener  entry = " +  ovpPlaylistIdOptions.playlistId)
            }
        }

        addPlayerListeners()
    }
}
