package com.biplio.jbmediabrowser.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.View.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.biplio.jbmediabrowser.R
import com.biplio.jbmediabrowser.utils.VIDEO_URL_KEY
import com.biplio.jbmediabrowser.utils.bounce
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
class VideoActivity : MediaBaseActivity(){
    
    private val TAG = VideoActivity::class.java.simpleName
    
    private val VIDEO_POSITION_KEY = "VIDEO_POSITION_KEY"

    private lateinit var valueAnimator: ValueAnimator
    private lateinit var videoView : PlayerView
    private lateinit var overlay : FrameLayout
    internal lateinit var closeButton : AppCompatImageView
    private lateinit var mRunnable: Runnable
    private val mHandler = Handler()
    
    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private lateinit var playbackStateListener: Player.Listener
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 27) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        }

        setContentView(R.layout.activity_video)

        videoView = findViewById(R.id.videoView)
        overlay = findViewById(R.id.overlay)
        closeButton = findViewById(R.id.close)
        closeButton.visibility = VISIBLE
        playbackStateListener = PlaybackStateListener()
    
        mRunnable = Runnable {
            closeButton.visibility = GONE
        }
        
        closeButton.setOnClickListener {
            onBackPressed()
        }
        
        videoView.setOnTouchListener { _, event ->
            closeButton.visibility = VISIBLE
            mHandler.postDelayed(mRunnable,3000)
            return@setOnTouchListener super.onTouchEvent(event)
        }
        valueAnimator = ValueAnimator.ofFloat(1F, 0F)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(VIDEO_POSITION_KEY, videoView.player?.currentPosition!!.toInt())
    }
    
    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(this, "myTC-Android")
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
    }
    
    internal fun showError() {
        val message = getString(R.string.p2_error_something_appears_to_have_gone_wrong)
        val snackBar = Snackbar.make(getSnackBarContainer(), message, Snackbar.LENGTH_LONG)

        val snackBarView = snackBar.view
        val textView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
        textView.textAlignment = TEXT_ALIGNMENT_CENTER
        snackBar.show()
        onBackPressed()
    }
    
    internal fun showAlertPositiveButtons(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.cannot_play_title))
        builder.setMessage(getString(R.string.cannot_play_msg))
        builder.setPositiveButton(getString(R.string.okay)){
                dialog, _ ->
            dialog.dismiss()
            onBackPressed()
        }
        val dialog: AlertDialog = builder.create()
        dialog.bounce()
        dialog.show()
    }

    private fun getSnackBarContainer() : View{
        return findViewById(R.id.coordinator)
    }
    
    inner class PlaybackStateListener : Player.Listener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_IDLE -> closeButton.visibility = VISIBLE
                ExoPlayer.STATE_BUFFERING -> closeButton.visibility = VISIBLE
                ExoPlayer.STATE_READY -> {
                    overlay.visibility = GONE
                    valueAnimator.duration = 300
                    valueAnimator.addUpdateListener{ valueAnimator ->
                        overlay.alpha = valueAnimator.animatedValue as Float
                    }
                    valueAnimator.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            overlay.visibility = GONE
                        }
                    })
                }
                ExoPlayer.STATE_ENDED -> onBackPressed()
                else -> showError()
            }
        }
        
        override fun onPlayerError(error: ExoPlaybackException) {
            closeButton.visibility = VISIBLE
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                val cause = error.sourceException
                if (cause is HttpDataSource.HttpDataSourceException) showAlertPositiveButtons()
                else showError()
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) closeButton.visibility = INVISIBLE
            else closeButton.visibility = VISIBLE
        }
    }
    
    private fun releasePlayer() {
        if (player != null) {
            playWhenReady = player!!.playWhenReady
            playbackPosition = player!!.currentPosition
            currentWindow = player!!.currentWindowIndex
            player?.release()
            player = null
        }
    }
    
    override fun onPause() {
        super.onPause()
        valueAnimator.cancel()
        if (Util.SDK_INT < 24) releasePlayer()
    }
    
    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) releasePlayer()
    }
    
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) initializePlayer()
    }
    
    override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT < 24 || player == null) initializePlayer()
    }
    
    private fun initializePlayer() {
        val videoUrl = intent.getStringExtra(VIDEO_URL_KEY)
        if (videoUrl != null) {
            player = SimpleExoPlayer.Builder(this@VideoActivity).build()
            videoView.player = player
            val uri = Uri.parse(videoUrl)
            val mediaSource = buildMediaSource(uri)
    
            player?.playWhenReady = playWhenReady
            player?.seekTo(currentWindow, playbackPosition)
            player?.addListener(playbackStateListener)
            player?.prepare(mediaSource, false, false)
        } else showError()
    }
    
    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        videoView.systemUiVisibility = (SYSTEM_UI_FLAG_LOW_PROFILE
                or SYSTEM_UI_FLAG_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_IMMERSIVE_STICKY or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}