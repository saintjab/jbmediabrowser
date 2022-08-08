package com.biplio.jbmediabrowser.uiviews

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Keep
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.sign

class GalleryImageView : AppCompatImageView{
    
    interface GalleryImageViewListener {
        fun onClick()
        fun onDismiss()
        fun enablePaging(enable: Boolean)
    }
    
    internal val SCALE = "scale"
    
    private val MAX_SCALE = 4f
    private val MIN_SCALE = 1f
    
    internal val HORIZONTAL_SCROLL_THRESHOLD = 0.25f
    private val BASE_CLOSE_GALLERY_THRESHOLD = 150f
    internal val DISMISS_IMAGE_THRESHOLD = 20f
    
    val TOP = -1
    val BOTTOM = 1
    val CENTRE = 0
    internal var mReleaseAnimationDirection = CENTRE
    internal var mCloseGalleryThreshold: Float = 0.toFloat()
    internal var mSwipeDirection: Float = 0.toFloat()
    internal var mSwipeInProgress = false
    
    
    internal val mScaleMatrix = Matrix()
    
    // Moving/ scaling states
    internal val NONE = 0
    internal val DRAG = 1
    internal val ZOOM = 2
    
    internal var mTouchMode = NONE
    internal val mInterpolator = DecelerateInterpolator()
    
    internal var mDismissAnimationRunning = false
    internal val mPreviousTouchPosition = PointF()
    internal val mInitialTouchPosition = PointF()
    internal val mMatrixValues = FloatArray(9)
    internal var mViewWidth: Int = 0
    internal var mViewHeight:Int = 0
    internal var mScalePivotX: Float = 0.toFloat()
    internal var mScalePivotY:Float = 0.toFloat()
    
    internal var mCurrentScale = 1f
    internal var mOrigWidth: Float = 0.toFloat()
    internal var mOrigHeight:Float = 0.toFloat()
    private var mPreviousMeasuredWidth: Int = 0
    private var mPreviousMeasuredHeight:Int = 0
    internal var mHorizontalMargin: Float = 0.toFloat()
    internal var mScaleDetector: ScaleGestureDetector? = null
    internal var mGestureDetector: GestureDetector? = null
    internal var mScaleAnimator = ObjectAnimator()
    internal var mListener: GalleryImageViewListener? = null
    internal var zoomEnabled: Boolean = false
    
    constructor(context: Context) : super(context){
        init()
    }
    
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        init()
    }
    
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs,
        defStyleAttr) {
        init()
    }
    
    private fun init() {
        isClickable = true
        mCloseGalleryThreshold = resources.displayMetrics.density * BASE_CLOSE_GALLERY_THRESHOLD
        mScaleDetector = ScaleGestureDetector(context, mScaleGestureDetector)
        mGestureDetector = GestureDetector(context, mGestureListener)
        fixTranslation()
        imageMatrix = (mScaleMatrix)
        scaleType = (ScaleType.MATRIX)
        setOnTouchListener(mTouchListener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mViewWidth = MeasureSpec.getSize(widthMeasureSpec)
        mViewHeight = MeasureSpec.getSize(heightMeasureSpec)
        
        if (mPreviousMeasuredHeight == mViewWidth && mPreviousMeasuredHeight == mViewHeight ||
            mViewWidth == 0 || mViewHeight == 0) {
            return
        }
        
        mPreviousMeasuredHeight = mViewHeight
        mPreviousMeasuredWidth = mViewWidth
        mCurrentScale = 1f
        
        val scale: Float
        val drawable = drawable
        
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            return
        }
        
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        val scaleX = mViewWidth.toFloat() / drawableWidth.toFloat()
        val scaleY = mViewHeight.toFloat() / drawableHeight.toFloat()
        
        scale = scaleX.coerceAtMost(scaleY)
        mScaleMatrix.setScale(scale, scale)
        
        // Center the image
        mHorizontalMargin = mViewWidth.toFloat() - scale * drawableWidth.toFloat()
        val mVerticalMargin = mViewHeight.toFloat() - scale * drawableHeight.toFloat()
        
        // Translate the image to centre based on margins above and to the side of the image
        mScaleMatrix.postTranslate(mHorizontalMargin / 2, mVerticalMargin / 2)
        
        mOrigWidth = mViewWidth - mHorizontalMargin
        mOrigHeight = mViewHeight - mVerticalMargin
        imageMatrix = (mScaleMatrix)
        fixTranslation()
    }
    
    private val mTouchListener = object : OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            if (!mDismissAnimationRunning) {
                mGestureDetector!!.onTouchEvent(event)
                
                if (drawable == null) return true
                
                if (zoomEnabled) mScaleDetector!!.onTouchEvent(event)
                
                val touchPosition = PointF(event.rawX, event.rawY)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mPreviousTouchPosition.set(touchPosition)
                        mInitialTouchPosition.set(mPreviousTouchPosition)
                        mTouchMode = DRAG
                    }
                    
                    MotionEvent.ACTION_MOVE -> if (mTouchMode == DRAG) {
                        val deltaX = touchPosition.x - mPreviousTouchPosition.x
                        val deltaY = touchPosition.y - mPreviousTouchPosition.y
                        
                        val fixTransX = getFixDragTrans(deltaX,
                            mViewWidth.toFloat(), mOrigWidth * mCurrentScale)
                        val fixTransY = getFixDragTrans(deltaY, mViewHeight.toFloat(),
                            mOrigHeight * mCurrentScale)
                        
                        val translationY = touchPosition.y - mInitialTouchPosition.y
                        if (mCurrentScale == 1f && (abs(translationY) > DISMISS_IMAGE_THRESHOLD || mSwipeInProgress)) {
                            if (!mSwipeInProgress) {
                                mSwipeInProgress = true
                                // Only assigning this variable once prevents a jerk when the user crosses the
                                // origin on further back and forth movements
                                mSwipeDirection = sign(translationY)
                            }
                            setTranslationY(translationY - mSwipeDirection * DISMISS_IMAGE_THRESHOLD)
                            if (abs(getTranslationY()) > mCloseGalleryThreshold) {
                                mReleaseAnimationDirection =
                                    if (getTranslationY() > 0) BOTTOM else TOP
                                animateReleasedImage(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        mListener!!.onDismiss()
                                    }
                                })
                            }
                        }
                        
                        mScaleMatrix.postTranslate(fixTransX, fixTransY)
                        fixTranslation()
                        mPreviousTouchPosition.set(touchPosition.x, touchPosition.y)
                        
                        mScaleMatrix.getValues(mMatrixValues)
                        
                        // This code allows you to page while zoomed in but android photos does not let you do this it makes you zoom all the way out first
                        val translationX = mMatrixValues[Matrix.MTRANS_X]
                        // Enable or disable scrolling the containing view pager based on the current position of the image
                        mListener?.enablePaging(((mCurrentScale == 1f || abs(translationX)
                                < HORIZONTAL_SCROLL_THRESHOLD ||
                                (translationX - mHorizontalMargin + ((mCurrentScale - 1) * mOrigWidth))
                                < HORIZONTAL_SCROLL_THRESHOLD)) &&
                                abs(getTranslationY()) == 0f)
                        mListener!!.enablePaging(mCurrentScale == 1f)
                    }
                    
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                        mTouchMode = NONE
                        mSwipeInProgress = false
                        mReleaseAnimationDirection = CENTRE
                        if (abs(translationY) > 0) {
                            animateReleasedImage(null)
                        }
                    }
                    
                    MotionEvent.ACTION_POINTER_UP -> mTouchMode = NONE
                }
                imageMatrix = (mScaleMatrix)
                invalidate()
            }
            return true
        }
    }
    
    internal fun animateReleasedImage(listener: AnimatorListenerAdapter?) {
        if (animation == null || !animation.hasStarted()) {
            mListener!!.enablePaging(false)
            mDismissAnimationRunning = true
            // add multiplier to the height to ensure the view animates completely off the screen
            animate().translationY(mReleaseAnimationDirection.toFloat() * 1.1f * mViewHeight.toFloat())
                .setDuration(300).setInterpolator(mInterpolator).setListener(object :
                    AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (mReleaseAnimationDirection == CENTRE) {
                            mDismissAnimationRunning = false
                        }
                        mListener!!.enablePaging(true)
                        listener?.onAnimationEnd(animation)
                    }
                }).start()
        }
    }
    
    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (zoomEnabled) {
                mScalePivotX = e.x
                mScalePivotY = e.y
                if (mScaleAnimator.isRunning) {
                    mScaleAnimator.cancel()
                }
    
                mScaleAnimator = if (mCurrentScale > 1) ObjectAnimator
                    .ofFloat(this@GalleryImageView, SCALE, 1F, 0.8f)
                
                else ObjectAnimator.ofFloat(this@GalleryImageView, SCALE, 1F, 1.2f)
                
                mScaleAnimator.duration = 400
                mScaleAnimator.interpolator = mInterpolator
                mScaleAnimator.start()
                
                return true
            }
            return false
        }
        
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            mListener?.onClick()
            return true
        }
    }
    
    private val mScaleGestureDetector = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleImage(detector.scaleFactor, detector.focusX, detector.focusY)
                return true
            }
            
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                mTouchMode = ZOOM
                return true
            }
        }
    
    internal fun scaleImage(scaleMultiplierX: Float, x: Float, y: Float) {
        var scaleMultiplier = scaleMultiplierX
        val origScale = mCurrentScale
        mCurrentScale *= scaleMultiplier
        
        if (mCurrentScale > MAX_SCALE) {
            mCurrentScale = MAX_SCALE
            scaleMultiplier = MAX_SCALE / origScale
        } else if (mCurrentScale < MIN_SCALE) {
            mCurrentScale = MIN_SCALE
            scaleMultiplier = MIN_SCALE / origScale
        }
        
        if (mOrigWidth * mCurrentScale <= mViewWidth || mOrigHeight * mCurrentScale <= mViewHeight) {
            mScaleMatrix.postScale(
                scaleMultiplier,
                scaleMultiplier,
                (mViewWidth / 2).toFloat(),
                (mViewHeight / 2).toFloat()
            )
        } else {
            mScaleMatrix.postScale(scaleMultiplier, scaleMultiplier, x, y)
        }
        fixTranslation()
    }
    
    internal fun fixTranslation() {
        mScaleMatrix.getValues(mMatrixValues)
        val transX = mMatrixValues[Matrix.MTRANS_X]
        val transY = mMatrixValues[Matrix.MTRANS_Y]
        val fixTransX =
            getFixedTranslation(transX, mViewWidth.toFloat(), mOrigWidth * mCurrentScale)
        val fixTransY =
            getFixedTranslation(transY, mViewHeight.toFloat(), mOrigHeight * mCurrentScale)
        if (fixTransX != 0f || fixTransY != 0f) {
            mScaleMatrix.postTranslate(fixTransX, fixTransY)
        }
    }
    
    private fun getFixedTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        
        if (trans < minTrans) {
            return -trans + minTrans
        }
        
        return if (trans > maxTrans) {
            -trans + maxTrans
        } else 0f
    }
    
    internal fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) 0f else delta
    }
    
    // Method required for double tap scale animation.
    @Keep
    fun setScale(scaleFactor: Float) {
        scaleImage(scaleFactor, mScalePivotX, mScalePivotY)
        imageMatrix = (mScaleMatrix)
        invalidate()
    }
    
    fun setListener(listener: GalleryImageViewListener) {
        mListener = listener
    }
    
    fun setZoomEnabled(zoomEnabled: Boolean) {
        this.zoomEnabled = zoomEnabled
    }
}