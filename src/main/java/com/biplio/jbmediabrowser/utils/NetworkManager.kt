package com.biplio.jbmediabrowser.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

class NetworkManager {
    companion object {
        fun isNetworkConnected(context: Context?): Boolean {
            val cnxManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val netInfo : NetworkInfo? = cnxManager?.activeNetworkInfo
            return netInfo?.isConnectedOrConnecting ?: false
        }

        
        fun isUsingMobileData(context: Context?) : Boolean{
            val cnxManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val netInfo : NetworkInfo? = cnxManager?.activeNetworkInfo
            return netInfo != null && netInfo.subtype != ConnectivityManager.TYPE_MOBILE
        }
    }
}