package com.beviswang.nfclibrary

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast

class MyService : Service() {

    private val mIBinder: IBinder = object : IMyAidlInterface.Stub() {
        override fun toastMsg(msg: String?): Boolean {
            Toast.makeText(this@MyService, msg, Toast.LENGTH_LONG).show()
            return true
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return mIBinder
    }
}
