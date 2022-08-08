package com.biplio.jbmediabrowser.others

interface MediaListener {
     fun toggleTitle(){}
     fun enablePaging(enabled: Boolean){}
     fun closeGallery(){}
     fun playVideo(mediaModel: MediaModel, warnDataUsage: Boolean = true)
}