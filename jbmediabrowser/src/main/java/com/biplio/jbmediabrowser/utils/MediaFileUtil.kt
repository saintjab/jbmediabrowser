package com.biplio.jbmediabrowser.utils

object MediaFileUtil {

    val VIDEOEX = listOf("mp4", "avi", "mkv", "wmv", "vob", "m4v", "amv", "mpd")
    val IMAGEEX = listOf("tif", "tiff", "gif", "png", "eps", "webp", "bmp")

    fun getFileNameWithExtension(url: String?) : Pair<String, MediaType> {
        val directory = url?.substringBeforeLast("/")
        val fullName = url?.substringAfterLast("/")
        val fileName = fullName?.substringBeforeLast(".")
        val extension = fullName?.substringAfterLast(".")
        val mediaType = when{
            VIDEOEX.contains(extension) -> MediaType.VIDEO
            IMAGEEX.contains(extension) -> MediaType.IMAGE
            else -> MediaType.UNKNOWN
        }
        return Pair("$fileName.$extension", mediaType)
    }
}