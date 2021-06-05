package com.biplio.jbmediabrowser.views

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.biplio.jbmediabrowser.R
import com.biplio.jbmediabrowser.others.MediaListener
import com.biplio.jbmediabrowser.others.MediaModel
import com.biplio.jbmediabrowser.uiviews.JBMediaViewPager
import com.biplio.jbmediabrowser.utils.*
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
class JBJBMediaActivity : JBMediaBaseActivity(), MediaListener {
    private val TAG = JBJBMediaActivity::class.java.simpleName
    
    private val ANIMATION_DURATION = 300
    private val FADE_OUT_DELAY = 3000
    internal var mediaItems = mutableListOf<MediaModel>()
    
    private var fadeAnimator = ObjectAnimator()
    private var fadeDelayHandler: Handler? = null
    private lateinit var viewPager: JBMediaViewPager
    internal lateinit var toolbarHelper : ToolbarHelper
    
    private var hasNetwork = false
    private var currentWindow: Int? = 0
    private var playbackPosition: Long? = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.MediaStyle)
        window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        if (Build.VERSION.SDK_INT >= 27) {
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jbmedia)

        fadeDelayHandler = Handler()

        intent.getStringArrayListExtra(MEDIA_KEY)?.forEach {
            val mediaType = MediaFileUtil.getFileNameWithExtension(it).second
            mediaItems.add(MediaModel(it, mediaType))
        }
        hasNetwork = hasNetwork()
        if (!mediaItems.isNullOrEmpty()) {
            viewPager = findViewById(R.id.gallery_view_pager)

            val galleryAdapter = JBMediaAdapter(supportFragmentManager, mediaItems)
            viewPager.adapter = galleryAdapter
            viewPager.addOnPageChangeListener(onPageChangeListener)

            toolbarHelper = ToolbarHelper(findViewById(R.id.toolbar), R.color.colorPrimaryDark)
            toolbarHelper.enableBackButton({ closeGallery() }, android.R.color.white)
            toolbarHelper.mToolbar.setTitleTextAppearance(
                this, R.style.GalleryTextAppearance_Medium)
            toolbarHelper.mToolbar.navigationContentDescription = getString(R.string.back)
        } else {
            Log.e(TAG, "String extra MEDIA_KEY was null, " +
                    "which should not happen. Closing GalleryFragment")
            finish()
        }
    }
    
    override fun onStart() {
        super.onStart()
        val mediaItemIndex = intent.getIntExtra(MEDIA_ITEM_INDEX_KEY, 0)
        viewPager.setCurrentItem(mediaItemIndex, true)
    }
    
    override fun onResume() {
        super.onResume()
        toolbarHelper.mToolbar.alpha = 1F
        toolbarHelper.setTitle(MediaFileUtil.getFileNameWithExtension(mediaItems[0].mediaURL as String).first)
    }
    
    override fun onPause() {
        super.onPause()
        fadeAnimator.cancel()
        fadeDelayHandler?.removeCallbacks(fadeOutRunnable)
    }
    
    override fun toggleTitle() {
        if (!fadeAnimator.isRunning) animateTitle((if (toolbarHelper.mToolbar.alpha == 1F) 0
        else 1).toFloat())
    }
    
    override fun enablePaging(enabled: Boolean) {
        viewPager.isEnabled = (enabled)
    }
    
    override fun closeGallery() {
        onBackPressed()
    }
    
    override fun playVideo(mediaModel: MediaModel, warnDataUsage: Boolean,
                           currentWindow: Int?, playbackPosition: Long?) {
        this.currentWindow = currentWindow
        this.playbackPosition = playbackPosition
        if (usingMobileData() && warnDataUsage) showDataUsageDialog(mediaModel)
        else play(mediaModel)
    }
    
    private val fadeOutRunnable = Runnable { toggleTitle() }
    
    private val onPageChangeListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(position: Int, positionOffset: Float,
                                    positionOffsetPixels: Int) {}
        override fun onPageSelected(position: Int) {
            toolbarHelper.setTitle(MediaFileUtil
                .getFileNameWithExtension(mediaItems[position].mediaURL as String).first)
        }
        
        override fun onPageScrollStateChanged(state: Int) {}
    }
    
    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    
    private fun animateTitle(toAlpha: Float) {
        fadeDelayHandler?.removeCallbacks(fadeOutRunnable)
        
        fadeAnimator = ObjectAnimator.ofFloat(toolbarHelper.mToolbar, View.ALPHA,
            toolbarHelper.mToolbar.alpha, toAlpha)
        fadeAnimator.duration = ANIMATION_DURATION.toLong()
        fadeAnimator.start()
        
        if (toAlpha == 1f)
            fadeDelayHandler?.postDelayed(fadeOutRunnable, (ANIMATION_DURATION + FADE_OUT_DELAY).toLong())
    }
    
    private fun showDataUsageDialog(mediaModel: MediaModel) {
        val builder = AlertDialog.Builder(ContextThemeWrapper(this, R.style.DialogTheme))
        builder.setTitle(R.string.mobile_data_warning_title)
        builder.setMessage(R.string.mobile_data_warning_message)
        builder.setPositiveButton(android.R.string.ok){
                dialog, _ ->  dialog.dismiss()
            play(mediaModel)
        }
        builder.setNegativeButton(getString(android.R.string.cancel)){
                dialog, _ ->  dialog.dismiss()
        }
        val dialog: AlertDialog = builder.create()
        dialog.bounce()
        dialog.show()
    }
    
    private fun play(mediaModel: MediaModel){
        val intent = Intent(this, VideoActivity::class.java)
        intent.putExtra(VIDEO_URL_KEY, mediaModel.mediaURL as String)
        intent.putExtra(CURRENT_WINDOW, currentWindow)
        intent.putExtra(PLAYBACK_POS, playbackPosition)
        startActivity(intent)
    }
}