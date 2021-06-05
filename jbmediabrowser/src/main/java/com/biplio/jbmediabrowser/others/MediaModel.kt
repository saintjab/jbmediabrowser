package com.biplio.jbmediabrowser.others

import android.os.Parcelable
import com.biplio.jbmediabrowser.utils.MediaType
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class MediaModel(val mediaURL: @RawValue Any, val mediaType: MediaType?) : Parcelable