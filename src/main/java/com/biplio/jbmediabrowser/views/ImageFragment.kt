package com.biplio.jbmediabrowser.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
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
import coil.load
import coil.request.ImageRequest
import coil.request.ImageResult
import com.biplio.jbmediabrowser.R
import com.biplio.jbmediabrowser.others.MediaListener
import com.biplio.jbmediabrowser.others.MediaModel
import com.biplio.jbmediabrowser.uiviews.GalleryImageView
import com.biplio.jbmediabrowser.utils.MEDIA_ITEM_KEY
import com.biplio.jbmediabrowser.utils.NO_NETWORK
import com.biplio.jbmediabrowser.utils.bounce
import com.google.android.exoplayer2.Player
import java.util.*

class ImageFragment :  Fragment(), GalleryImageView.GalleryImageViewListener{
    private val TAG = ImageFragment::class.java.simpleName
    private var mediaModel: MediaModel? = null
    
    private lateinit var progressBar: View
    private lateinit var viewRoot: View
    private var isErrorShown = false
    private var galleryImageView: GalleryImageView? = null
    private val animators = ArrayList<Animator>()

    private var errorImage = R.drawable.error_image
    
    companion object {
        fun newInstance(mediaModel: MediaModel) = ImageFragment().apply{
            arguments = Bundle().apply {
                putParcelable(MEDIA_ITEM_KEY, mediaModel)
            }
        }
    }
    
    private fun getGalleryListener(): MediaListener? {
        return try {
            activity as MediaListener
        }
        catch (ex : Exception){
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
        viewRoot = inflater.inflate(R.layout.fragment_gallery_zoom, container, false)
        galleryImageView = viewRoot.findViewById(R.id.gallery_image)
        progressBar = viewRoot.findViewById(R.id.progressBar)
        return viewRoot
    }
    
    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        galleryImageView?.setListener(this)
        galleryImageView?.setZoomEnabled(true)
        loadImage()
    }
    
    override fun onPause() {
        super.onPause()
        for (animator in ArrayList(animators)) {
            animator.cancel()
        }
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
    
    private fun loadImage() {
        galleryImageView?.load(mediaModel?.mediaURL as String) {
            listener(
                onStart = {
                    Log.d(TAG, "onStart")
                },
                onSuccess = { request: ImageRequest, _: ImageResult.Metadata ->
                    Log.d(TAG, "onSuccess")
                    if (request.data != null) progressBar.visibility = GONE
                    else fadeView(progressBar, 0f, null)
                }, onError = { _, throwable: Throwable ->
                    Log.d(TAG, "onError ${throwable.localizedMessage}")
                    showError(errorImage, throwable)
                }
            )
        }
    }

    private fun showError(errorImage: Int, throwable: Throwable) {
        fadeView(progressBar, 0f, null)
        galleryImageView?.setImageResource(errorImage)
        if (!isErrorShown && throwable.toString().contains(NO_NETWORK)) {
            isErrorShown = true
            showAlertPositiveButtons(getString(R.string.your_are_not_connected))
        }
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
    
    override fun onClick() {
        getGalleryListener()?.toggleTitle()
    }
    
    override fun onDismiss() {
        getGalleryListener()?.closeGallery()
    }
    
    override fun enablePaging(enable: Boolean) {
        getGalleryListener()?.enablePaging(enable)
    }
}