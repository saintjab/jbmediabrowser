package com.biplio.jbmediabrowser.utils

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.biplio.jbmediabrowser.utils.animation.BounceView
import java.util.*


fun Int.dpToPx(context: Context): Int {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), metrics).toInt()
}

fun Bitmap.createBitmapWithBorder(borderSize: Float, borderColor: Int = Color.WHITE): Bitmap {
    val borderOffset = (borderSize * 2).toInt()
    val halfWidth = width / 2
    val halfHeight = height / 2
    val circleRadius = Math.min(halfWidth, halfHeight).toFloat()
    val newBitmap = Bitmap.createBitmap(
        width + borderOffset,
        height + borderOffset,
        Bitmap.Config.ARGB_8888
    )
    
    // Center coordinates of the image
    val centerX = halfWidth + borderSize
    val centerY = halfHeight + borderSize
    
    val paint = Paint()
    val canvas = Canvas(newBitmap).apply {
        // Set transparent initial area
        drawARGB(0, 0, 0, 0)
    }
    
    // Draw the transparent initial area
    paint.isAntiAlias = true
    paint.style = Paint.Style.FILL
    canvas.drawCircle(centerX, centerY, circleRadius, paint)
    
    // Draw the image
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, borderSize, borderSize, paint)
    
    // Draw the createBitmapWithBorder
    paint.xfermode = null
    paint.style = Paint.Style.STROKE
    paint.color = borderColor
    paint.strokeWidth = borderSize
    canvas.drawCircle(centerX, centerY, circleRadius, paint)
    return newBitmap
}

fun FragmentManager.setupForAccessibility() {
    addOnBackStackChangedListener {
        try{
            val lastFragmentWithView = fragments.last { it.view != null }
            for (fragment in fragments) {
                if (fragment == lastFragmentWithView) {
                    fragment.view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                } else {
                    fragment.view?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                }
            }
        }
        catch (xx: Exception){ }
    }
}

fun View.bounce(){
    val bounce = BounceView(this)
    bounce.addAnimTo(this)
}

fun AlertDialog.bounce(){
    val bounce = BounceView(this)
    bounce.addAnimTo(this)
}

fun Date.expired(days: Int): Boolean {
    val c = Calendar.getInstance()
    c.time = this
    c.add(Calendar.DATE, days)
    return c.time <= Date()
}

fun Int.toDp(context: Context):Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,this.toFloat(),context.resources.displayMetrics).toInt()

fun Int.toString(context: Context): String {
    return context.getString(this)
}



