package com.biplio.jbmediabrowser.utils.animation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.PopupWindow
import com.biplio.jbmediabrowser.R
import com.google.android.material.tabs.TabLayout
import java.lang.ref.WeakReference



/**
 * Created by Jonas Boateng - 23/11/2020
 * Adapted from hari.bounceview:bounceview
 */
class BounceView : BounceViewAnim {
    private val PUSH_IN_SCALE_X = 0.9f
    private val PUSH_IN_SCALE_Y = 0.9f
    private val POP_OUT_SCALE_X = 1.1f
    private val POP_OUT_SCALE_Y = 1.1f
    private val PUSH_IN_ANIM_DURATION = 100
    private val POP_OUT_ANIM_DURATION = 100
    internal val DEFAULT_INTERPOLATOR = AccelerateDecelerateInterpolator()
    
    private var view: WeakReference<View>? = null
    private var dialog: WeakReference<Dialog>? = null
    private var popup: WeakReference<PopupWindow>? = null
    private var tabLayout: WeakReference<TabLayout>? = null
    internal var isTouchInsideView = true
    internal var pushInScaleX = PUSH_IN_SCALE_X
    internal var pushInScaleY = PUSH_IN_SCALE_Y
    internal var popOutScaleX = POP_OUT_SCALE_X
    internal var popOutScaleY = POP_OUT_SCALE_Y
    internal var pushInAnimDuration = PUSH_IN_ANIM_DURATION
    internal var popOutAnimDuration = POP_OUT_ANIM_DURATION
    internal var pushInInterpolator = DEFAULT_INTERPOLATOR
    internal var popOutInterpolator = DEFAULT_INTERPOLATOR
    
    constructor(view: View) {
        this.view = WeakReference<View>(view)
        if (this.view!!.get() != null) {
            if (!this.view!!.get()?.hasOnClickListeners()!!) {
                this.view!!.get()?.setOnClickListener { }
            }
        }
    }
    
    constructor(dialog: Dialog) {
        this.dialog = WeakReference<Dialog>(dialog)
    }
    
    constructor(popup: PopupWindow) {
        this.popup = WeakReference<PopupWindow>(popup)
    }
    
    constructor(tabLayout: TabLayout) {
        this.tabLayout = WeakReference<TabLayout>(tabLayout)
    }
    
    
    fun addAnimTo(view: View?): BounceView? {
        val bounceAnim = view?.let {
            BounceView(it)
        }
        bounceAnim?.setAnimToView()
        return bounceAnim
    }
    
    fun addAnimTo(dialog: Dialog?) {
        dialog?.let { BounceView(it) }?.setAnimToDialog()
    }
    
    fun addAnimTo(popupWindow: PopupWindow?) {
        popupWindow?.let { BounceView(it) }?.setAnimToPopup()
    }
    
    fun addAnimTo(tabLayout: TabLayout?): BounceView? {
        val bounceAnim = tabLayout?.let { BounceView(it) }
        bounceAnim?.setAnimToTabLayout()
        return bounceAnim
    }
    
    override fun setScaleForPushInAnim(scaleX: Float, scaleY: Float): BounceViewAnim? {
        pushInScaleX = scaleX
        pushInScaleY = scaleY
        return this
    }
    
    override fun setScaleForPopOutAnim(scaleX: Float, scaleY: Float): BounceViewAnim {
        popOutScaleX = scaleX
        popOutScaleY = scaleY
        return this
    }
    
    override fun setPushInAnimDuration(timeInMillis: Int): BounceViewAnim {
        pushInAnimDuration = timeInMillis
        return this
    }
    
    override fun setPopOutAnimDuration(timeInMillis: Int): BounceViewAnim {
        popOutAnimDuration = timeInMillis
        return this
    }
    
    override fun setInterpolatorPushIn(interpolatorPushIn: AccelerateDecelerateInterpolator?): BounceViewAnim {
        if (interpolatorPushIn != null) {
            pushInInterpolator = interpolatorPushIn
        }
        return this
    }
    
    override fun setInterpolatorPopOut(interpolatorPopOut: AccelerateDecelerateInterpolator?): BounceViewAnim {
        if (interpolatorPopOut != null) {
            popOutInterpolator = interpolatorPopOut
        }
        return this
    }
    
    private fun setAnimToView() {
        if (view != null) {
            view!!.get()?.setOnTouchListener(object : View.OnTouchListener {
                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(v: View, motionEvent: MotionEvent): Boolean {
                    val action = motionEvent.action
                    if (action == MotionEvent.ACTION_DOWN) {
                        isTouchInsideView = true
                        startAnimScale(
                            v,
                            pushInScaleX,
                            pushInScaleY,
                            pushInAnimDuration,
                            pushInInterpolator,
                            0
                        )
                    } else if (action == MotionEvent.ACTION_UP) {
                        if (isTouchInsideView) {
                            v.animate().cancel()
                            startAnimScale(
                                v,
                                popOutScaleX,
                                popOutScaleY,
                                popOutAnimDuration,
                                popOutInterpolator,
                                0
                            )
                            startAnimScale(
                                v,
                                1f,
                                1f,
                                popOutAnimDuration,
                                popOutInterpolator,
                                popOutAnimDuration + 1
                            )
                            return false
                        }
                    } else if (action == MotionEvent.ACTION_CANCEL) {
                        if (isTouchInsideView) {
                            v.animate().cancel()
                            startAnimScale(v, 1f, 1f, popOutAnimDuration, DEFAULT_INTERPOLATOR, 0)
                        }
                        return true
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        if (isTouchInsideView) {
                            val currentX = motionEvent.x
                            val currentY = motionEvent.y
                            val currentPosX: Float = currentX + v.left
                            val currentPosY: Float = currentY + v.top
                            val viewLeft: Int = v.left
                            val viewTop: Int = v.top
                            val viewRight: Int = v.right
                            val viewBottom: Int = v.bottom
                            if (!(currentPosX > viewLeft && currentPosX < viewRight && currentPosY > viewTop && currentPosY < viewBottom)) {
                                isTouchInsideView = false
                                v.animate().cancel()
                                startAnimScale(
                                    v,
                                    1f,
                                    1f,
                                    popOutAnimDuration,
                                    DEFAULT_INTERPOLATOR,
                                    0
                                )
                            }
                            return true
                        }
                    }
                    return false
                }
            })
        }
    }
    
    internal fun startAnimScale(
        view: View, scaleX: Float, scaleY: Float,
        animDuration: Int,
        interpolator: AccelerateDecelerateInterpolator,
        startDelay: Int
    ) {
        val animX = ObjectAnimator.ofFloat(view, "scaleX", scaleX)
        val animY = ObjectAnimator.ofFloat(view, "scaleY", scaleY)
        val animatorSet = AnimatorSet()
        animX.duration = animDuration.toLong()
        animX.interpolator = interpolator
        animY.duration = animDuration.toLong()
        animY.interpolator = interpolator
        animatorSet.playTogether(animX, animY)
        animatorSet.startDelay = startDelay.toLong()
        animatorSet.start()
    }
    
    private fun setAnimToDialog() {
        if (dialog?.get() != null) {
            dialog!!.get()?.window?.setWindowAnimations(R.style.CustomDialogAnimation)
        }
    }
    
    private fun setAnimToPopup() {
        if (popup?.get() != null) {
            popup!!.get()?.animationStyle = R.style.CustomDialogAnimation
        }
    }
    
    private fun setAnimToTabLayout() {
        if (tabLayout?.get() != null) {
            for (i in 0 until (tabLayout!!.get()?.tabCount ?: 0)) {
                val tab: TabLayout.Tab? = tabLayout!!.get()?.getTabAt(i)
                val tabView: View = (tabLayout!!.get()?.getChildAt(0) as ViewGroup).getChildAt(i)
                tabView.setOnTouchListener(object : View.OnTouchListener {
                    @SuppressLint("ClickableViewAccessibility")
                    override fun onTouch(v: View, motionEvent: MotionEvent): Boolean {
                        val action = motionEvent.action
                        if (action == MotionEvent.ACTION_DOWN) {
                            isTouchInsideView = true
                            startAnimScale(
                                v,
                                pushInScaleX,
                                pushInScaleY,
                                pushInAnimDuration,
                                pushInInterpolator,
                                0
                            )
                            return true
                        } else if (action == MotionEvent.ACTION_UP) {
                            if (isTouchInsideView) {
                                v.animate().cancel()
                                startAnimScale(
                                    v,
                                    popOutScaleX,
                                    popOutScaleY,
                                    popOutAnimDuration,
                                    popOutInterpolator,
                                    0
                                )
                                startAnimScale(
                                    v,
                                    1f,
                                    1f,
                                    popOutAnimDuration,
                                    popOutInterpolator,
                                    popOutAnimDuration + 1
                                )
                                if (tab != null) {
                                    tab.select()
                                }
                                return false
                            }
                        } else if (action == MotionEvent.ACTION_CANCEL) {
                            if (isTouchInsideView) {
                                v.animate().cancel()
                                startAnimScale(
                                    v,
                                    1f,
                                    1f,
                                    popOutAnimDuration,
                                    DEFAULT_INTERPOLATOR,
                                    0
                                )
                            }
                            return true
                        } else if (action == MotionEvent.ACTION_MOVE) {
                            if (isTouchInsideView) {
                                val currentX = motionEvent.x
                                val currentY = motionEvent.y
                                val currentPosX: Float = currentX + v.left
                                val currentPosY: Float = currentY + v.top
                                val viewLeft: Int = v.left
                                val viewTop: Int = v.top
                                val viewRight: Int = v.right
                                val viewBottom: Int = v.bottom
                                if (!(currentPosX > viewLeft && currentPosX < viewRight && currentPosY > viewTop && currentPosY < viewBottom)) {
                                    isTouchInsideView = false
                                    v.animate().cancel()
                                    startAnimScale(
                                        v,
                                        1f,
                                        1f,
                                        popOutAnimDuration,
                                        DEFAULT_INTERPOLATOR,
                                        0
                                    )
                                }
                                return true
                            }
                        }
                        return false
                    }
                })
            }
        }
    }
}