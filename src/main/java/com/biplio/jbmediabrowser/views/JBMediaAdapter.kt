package com.biplio.jbmediabrowser.views

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.biplio.jbmediabrowser.others.MediaModel
import com.biplio.jbmediabrowser.utils.MediaType

class JBMediaAdapter (fm: FragmentManager, private val mediaItems: List<MediaModel>): FragmentPagerAdapter(fm,
    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    
    override fun getItem(position: Int): Fragment {
        val mediaItem = mediaItems[position]

        return if (mediaItem.mediaType == MediaType.VIDEO)
            VideoFragment.newInstance(mediaItems[position])
        else ImageFragment.newInstance(mediaItems[position])
    }
    
    override fun getCount(): Int {
        return mediaItems.size
    }
}