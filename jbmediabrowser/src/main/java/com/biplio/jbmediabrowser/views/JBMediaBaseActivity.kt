package com.biplio.jbmediabrowser.views

import androidx.appcompat.app.AppCompatActivity
import com.biplio.jbmediabrowser.utils.NetworkManager
import kotlinx.coroutines.ExperimentalCoroutinesApi


@ExperimentalCoroutinesApi
abstract class JBMediaBaseActivity : AppCompatActivity(){
    
    private val TAG = JBMediaBaseActivity::class.java.simpleName
    
    fun usingMobileData(): Boolean {
        return NetworkManager.isUsingMobileData(this)
    }
    
    fun hasNetwork() : Boolean{
        return NetworkManager.isNetworkConnected(this)
    }
}