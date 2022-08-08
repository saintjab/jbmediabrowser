package com.biplio.jbmediabrowser.uiviews

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import androidx.annotation.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.biplio.jbmediabrowser.R
import java.util.*

class JBToolbar: Toolbar{
    
    interface Tintable {
        fun setTintColor(@ColorInt tintColor: Int)
    }
    
    private class DrawableTintWrapper(private val mDrawable: Drawable) : Tintable {
        override fun setTintColor(@ColorInt tintColor: Int) {
            mDrawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
        }
    }
    
    private var title: AppCompatTextView? = null
    private var menuContainer: LinearLayout? = null
    private var mCollapsed: Boolean = false
    private var hideMenuIconsOnCollapse: Boolean = false
    private var hideMenuIconsOnExpand: Boolean = false
    private val tinTables = HashMap<View, Tintable>()
    
    constructor(context: Context) : super(context){
        init(context, null, 0)
    }
    
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        init(context, attrs, 0)
    }
    
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): this(context, attrs) {
        init(context, attrs, defStyleAttr)
    }
    
    private fun init(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.JBToolbar, defStyle, 0)
        try {
        
        } finally {
            a.recycle()
        }
        
        val layoutInflater = LayoutInflater.from(getContext())
        val view = layoutInflater.inflate(R.layout.toolbar, this, true)
        
        menuContainer = view.findViewById(R.id.menu_icon_container)
        title = view.findViewById(R.id.title) as AppCompatTextView
        
        overrideSuperViews()
    }
    
    private fun overrideSuperViews() {
        title?.text = super.getTitle()
        super.setTitle(null)
    }
    
    override fun setTitle(@StringRes resId: Int) {
        title?.setText(resId)
    }
    
    override fun setTitle(title: CharSequence) {
        this.title?.text = title
    }
    
    override fun setTitleTextAppearance(context: Context, @StyleRes resId: Int) {
        title?.setTextAppearance(context, resId)
    }
    
    override fun setTitleTextColor(@ColorInt color: Int) {
        title?.setTextColor(color)
    }
    
    fun addCustomMenuItem(view: View) {
        val tintable = if (view is Tintable) view else null
        addCustomMenuItem(view, tintable)
    }
    
    fun addCustomMenuItem(view: View, tintable: Tintable?) {
        menuContainer!!.addView(view)
        tinTables[view] = tintable!!
    }
    
    fun setHideMenuIconsOnCollapse(hideMenuIconsOnCollapse: Boolean) {
        this.hideMenuIconsOnCollapse = hideMenuIconsOnCollapse
    }
    
    fun setHideMenuIconsOnExpand(hideMenuIconsOnExpand: Boolean) {
        this.hideMenuIconsOnExpand = hideMenuIconsOnExpand
    }
    
    fun isCollapsed(): Boolean {
        return mCollapsed
    }
    
    fun setCollapsed(collapsed: Boolean, animated: Boolean) {
        mCollapsed = collapsed
        
        val menu = menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            @IdRes val itemId = item.itemId
            val itemView = findViewById<View>(itemId)
            
            setIconVisible(itemView, collapsed, animated)
        }
        
        for (i in 0 until menuContainer!!.childCount) {
            val child = menuContainer!!.getChildAt(i)
            val tintable = tinTables[child]
            if (tintable != null) {
                if (collapsed && hideMenuIconsOnCollapse || !collapsed && hideMenuIconsOnExpand) {
                    setIconVisibility(child, false, animated)
                } else {
                    setIconVisibility(child, true, animated)
                }
                setIconCollapsed(tintable, collapsed, animated, android.R.color.white,
                    android.R.color.black)
            }
        }
        
        val navIcon = navigationIcon
        if (navIcon != null) {
            val tintable = DrawableTintWrapper(navIcon)
            setIconCollapsed(tintable, collapsed, animated, android.R.color.white,
                R.color.colorPrimaryDark)
        }
    }
    
    private fun setIconVisibility(icon: View, visible: Boolean, animated: Boolean) {
        if (animated && icon.visibility == View.VISIBLE) {
            
            icon.isEnabled = visible
            icon.animate().alpha(if (visible) 1f else 0f).setDuration(300).start()
            
        } else {
            setIconVisibility(icon, visible)
        }
    }
    
    private fun setIconVisibility(icon: View, visible: Boolean) {
        icon.isEnabled = visible
        icon.alpha = (if (visible) 1 else 0).toFloat()
    }
    
    private fun setIconCollapsed(tintable: Tintable, collapsed: Boolean,
        animated: Boolean, @ColorRes expandedColor: Int, @ColorRes collapsedColor: Int) {
        
        val fromColor = if (collapsed) expandedColor else collapsedColor
        val toColor = if (collapsed) collapsedColor else expandedColor
        val from = ContextCompat.getColor(context, fromColor)
        val to = ContextCompat.getColor(context, toColor)
        
        if (animated) {
            animateDrawableColor(tintable, from, to)
        } else {
            tintable.setTintColor(to)
        }
    }
    
    private fun setIconVisible(icon: View, visible: Boolean, animated: Boolean) {
        val startAlpha = if (visible) 0.0f else 1.0f
        val endAlpha = 1.0f - startAlpha
        if (icon.visibility != View.GONE) {
            if (animated) {
                icon.visibility = View.VISIBLE
                icon.startAnimation(getAlphaAnimation(startAlpha, endAlpha))
            } else {
                icon.visibility = View.INVISIBLE
            }
            icon.isEnabled = visible
        }
        
    }
    
    private fun animateDrawableColor(tintable: Tintable, @ColorInt fromColor: Int, @ColorInt toColor: Int): Animator {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        animator.addUpdateListener { valueAnimator -> tintable.setTintColor(valueAnimator.animatedValue as Int) }
        
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {
                tintable.setTintColor(fromColor)
            }
            
            override fun onAnimationEnd(animator: Animator) {
                tintable.setTintColor(toColor)
            }
            
            override fun onAnimationCancel(animator: Animator) {
                tintable.setTintColor(toColor)
            }
            
            override fun onAnimationRepeat(animator: Animator) {
                tintable.setTintColor(fromColor)
            }
        })
        
        animator.duration = (resources.getInteger(R.integer.collapsing_layout_scrim_animation_duration).toLong())
        
        animator.start()
        return animator
    }
    
    private fun getAlphaAnimation(fromAlpha: Float, toAlpha: Float): Animation {
        val alphaAnimation = AlphaAnimation(fromAlpha, toAlpha)
        alphaAnimation.duration = (resources.getInteger(R.integer.collapsing_layout_scrim_animation_duration).toLong())
        alphaAnimation.fillAfter = true
        return alphaAnimation
    }
}