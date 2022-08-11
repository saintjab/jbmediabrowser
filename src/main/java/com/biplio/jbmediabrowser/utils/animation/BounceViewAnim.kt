package com.biplio.jbmediabrowser.utils.animation

import android.view.animation.AccelerateDecelerateInterpolator

/**
 * Created by Jonas Boateng - 23/11/2020
 * Adapted from hari.bounceview:bounceview
 */
interface BounceViewAnim {
    fun setScaleForPushInAnim(scaleX: Float, scaleY: Float): BounceViewAnim?
    
    fun setScaleForPopOutAnim(scaleX: Float, scaleY: Float): BounceViewAnim?
    
    fun setPushInAnimDuration(timeInMillis: Int): BounceViewAnim?
    
    fun setPopOutAnimDuration(timeInMillis: Int): BounceViewAnim?
    
    fun setInterpolatorPushIn(interpolatorPushIn: AccelerateDecelerateInterpolator?): BounceViewAnim?
    
    fun setInterpolatorPopOut(interpolatorPopOut: AccelerateDecelerateInterpolator?): BounceViewAnim?
}