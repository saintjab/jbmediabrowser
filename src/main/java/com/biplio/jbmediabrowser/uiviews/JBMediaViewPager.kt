package com.biplio.jbmediabrowser.uiviews

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class JBMediaViewPager: ViewPager {
    
    constructor(context: Context): super(context)
    
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return !isEnabled || super.onTouchEvent(event)
    }
    
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return isEnabled && super.onInterceptTouchEvent(event)
    }
}