package com.biplio.jbmediabrowser.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import com.biplio.jbmediabrowser.R
import com.biplio.jbmediabrowser.others.MediaListener
import com.biplio.jbmediabrowser.others.MediaModel
import com.biplio.jbmediabrowser.utils.MEDIA_ITEM_KEY
import com.biplio.jbmediabrowser.utils.NO_NETWORK
import com.biplio.jbmediabrowser.utils.bounce
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import java.util.*

class JBVideoFragment:  Fragment(){
    private val TAG = JBVideoFragment::class.java.simpleName
    private var mediaModel: MediaModel? = null
    
    private lateinit var progressBar: View
    private lateinit var viewRoot: View
    private var isErrorShown = false
    private lateinit var videoView: PlayerView
    private val animators = ArrayList<Animator>()
    private var player: SimpleExoPlayer? = null

    private var playWhenReady: Boolean? = true
    private var currentWindow: Int? = 0
    private var playbackPosition: Long? = 0
    private var errorImage = R.drawable.error_video
    private lateinit var playbackStateListener: Player.Listener
    
    companion object {
        fun newInstance(mediaModel: MediaModel) = JBVideoFragment().apply{
            arguments = Bundle().apply {
                putParcelable(MEDIA_ITEM_KEY, mediaModel)
            }
        }
    }
    
    private fun getGalleryListener(): MediaListener? {
        return try {
            activity as MediaListener
        }
        catch (ex: Exception){
            null
        }
    }
    
    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaModel = arguments?.getParcelable(MEDIA_ITEM_KEY)
    }
    
    @Nullable
    override fun onCreateView(inflater: LayoutInflater, @Nullable container: ViewGroup?,
                              @Nullable savedInstanceState: Bundle?): View {
        viewRoot = inflater.inflate(R.layout.fragment_jbvideo, container, false)
        progressBar = viewRoot.findViewById(R.id.progressBar)
        videoView = viewRoot.findViewById(R.id.videoView)

        playbackStateListener = PlaybackStateListener()
        return viewRoot
    }
    
    private fun showAlertPositiveButtons(message : String){
        val builder = AlertDialog.Builder(ContextThemeWrapper(context, R.style.DialogTheme))
        builder.setMessage(message)
        builder.setPositiveButton(getString(R.string.okay)){
                dialog, _ ->
            isErrorShown = false
            dialog.dismiss()
        }
        val dialog: AlertDialog = builder.create()
        dialog.bounce()
        dialog.show()
    }

    private fun showError(errorImage: Int, throwable: Throwable) {
        fadeView(progressBar, 0f, null)
        if (!isErrorShown && throwable.toString().contains(NO_NETWORK)) {
            isErrorShown = true
            showAlertPositiveButtons(getString(R.string.your_are_not_connected))
        }
        else showAlertPositiveButtons(getString(R.string.unable_to_play_video))
    }

    private fun fadeView(view: View?, toAlpha: Float, listener: Animator.AnimatorListener?) {
        view?.let {
            if (toAlpha == 1f) {
                view.alpha = 0f
                view.visibility = View.VISIBLE
            }
    
            val valueAnimator = ValueAnimator.ofFloat(view.alpha, toAlpha)
            valueAnimator.duration = 300
            valueAnimator.addUpdateListener {
                view.alpha = it.animatedValue as Float
            }
    
            valueAnimator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animators.remove(animation)
                    if (toAlpha == 0f) {
                        view.alpha = 1f
                        view.visibility = GONE
                    }
                    listener?.onAnimationEnd(animation)
                }
            })
            animators.add(valueAnimator)
            valueAnimator.start()
        }
    }

    inner class PlaybackStateListener : Player.Listener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                ExoPlayer.STATE_IDLE -> fadeView(progressBar, 0f, null)
                ExoPlayer.STATE_BUFFERING -> fadeView(progressBar, 0f, null)
                ExoPlayer.STATE_READY -> progressBar.visibility = GONE
                ExoPlayer.STATE_ENDED -> releasePlayer()
                else -> showError(errorImage, Throwable("Error playing video"))
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                val cause = error.sourceException
                if (cause is HttpDataSource.HttpDataSourceException)
                    showAlertPositiveButtons(cause.localizedMessage)
                else showError(errorImage, Throwable(error.localizedMessage))
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {}
    }

    private fun releasePlayer() {
        if (player != null) {
            playWhenReady = player?.playWhenReady
            playbackPosition = player?.currentPosition
            currentWindow = player?.currentWindowIndex
            player?.stop()
            player?.release()
            player = null
        }
    }

    override fun onPause() {
        super.onPause()
        for (animator in ArrayList(animators)) {
            animator.cancel()
        }
        Log.e(TAG, "onPause, playbackPosition $playbackPosition")
        releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        Log.e(TAG, "onResume, playbackPosition $playbackPosition")
        initializePlayer()
    }

    private fun initializePlayer() {
        videoView.setOnClickListener {
            Log.e(TAG, "Video onClick")
            releasePlayer()
            getGalleryListener()?.playVideo(mediaModel!!, true)
        }
        if (mediaModel?.mediaURL != null) {
            player = SimpleExoPlayer.Builder(requireActivity()).build()
            videoView.player = player
            val uri = Uri.parse(mediaModel?.mediaURL as String)
            val mediaSource = buildMediaSource(uri)
            player?.playWhenReady = playWhenReady!!
            player?.seekTo(currentWindow!!, playbackPosition!!)
            player?.addListener(playbackStateListener)
            player?.prepare(mediaSource, false, false)
        } else showError(errorImage, Throwable("No video found"))
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(requireActivity(), "JBAndroidBrowser")
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        videoView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }
}