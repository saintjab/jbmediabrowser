package com.biplio.jbmediabrowser.views

import android.content.Context
import android.content.Intent
import com.biplio.jbmediabrowser.utils.AUTO_PLAY_FIRST_VIDEO
import com.biplio.jbmediabrowser.utils.MEDIA_KEY

class JBMediaBrowser(context: Context, media: ArrayList<String>,
                     autoPlayVideo: Boolean = true){
    init {
        val intent = Intent(context, JBMediaActivity::class.java)
        intent.putStringArrayListExtra(MEDIA_KEY, ArrayList(media))
        intent.putExtra(AUTO_PLAY_FIRST_VIDEO, autoPlayVideo)
        context.startActivity(intent)
    }
}
