package com.biplio.jbmediabrowser.utils

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.annotation.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.biplio.jbmediabrowser.R
import com.biplio.jbmediabrowser.uiviews.JBToolbar

@RequiresApi(Build.VERSION_CODES.CUPCAKE)
class ToolbarHelper(var mToolbar: Toolbar, toolbarColorInt: Int) {
    
    private abstract class NavAction(@param:DrawableRes private val mNavIcon: Int) {
        fun getIcon(context: Context, @ColorRes color: Int): Drawable? {
            var icon = ContextCompat.getDrawable(context, mNavIcon)
            icon = icon?.mutate()?.constantState?.newDrawable()
            val tcRaspberryColor = ContextCompat.getColor(context, color)
            icon?.mutate()?.setColorFilter(tcRaspberryColor, PorterDuff.Mode.SRC_IN)
            return icon
        }
        
        abstract fun onClick()
    }
    
    init {
        setBackEnabled(false, null, toolbarColorInt)
    }
    
    fun enableBackButton(onBackPressed: Runnable, colorId: Int) {
        setBackEnabled(true, onBackPressed, colorId)
    }
    
    fun disableBackButton(colorId: Int) {
        setBackEnabled(false, null, colorId)
    }
    
    private fun setBackEnabled(backEnabled: Boolean, onBackPressed: Runnable?, colorId: Int) {
        if (backEnabled) {
            configureToolbar(object : NavAction(R.drawable.ic_arrow_back_white_24dp) {
                override fun onClick() {
                    onBackPressed?.run()
                }
            }, colorId)
        }
        else {
            configureToolbar(object : NavAction(R.drawable.ic_arrow_back_white_24dp) {
                override fun onClick() {
                }
            }, colorId)
        }
    }
    
    private fun configureToolbar(action: NavAction, colorId: Int) {
        mToolbar.navigationIcon = (action.getIcon(getContext(), colorId))
        mToolbar.setTitleTextAppearance(getContext(), R.style.JBDesign_Std_ToolbarTitle)
        mToolbar.setNavigationOnClickListener{ action.onClick() }
    }
    
    private fun getCollapsingToolbar(): JBToolbar {
        return mToolbar as JBToolbar
    }
    
    private fun getContext(): Context {
        return mToolbar.context
    }
    
    fun setTitle(@StringRes titleId: Int) {
        mToolbar.setTitle(titleId)
    }
    
    fun setTitle(title: String) {
        mToolbar.title = (title)
    }
    
    fun setMenu(@MenuRes menuId: Int) {
        mToolbar.inflateMenu(menuId)
    }
    
    fun setMenuOnClickListener(listener: Toolbar.OnMenuItemClickListener) {
        mToolbar.setOnMenuItemClickListener(listener)
    }
    
    fun addCustomMenuItem(view: View) {
        getCollapsingToolbar().addCustomMenuItem(view)
    }
    
    fun addCustomMenuItem(view: View, tintable: JBToolbar.Tintable) {
        getCollapsingToolbar().addCustomMenuItem(view, tintable)
    }
}