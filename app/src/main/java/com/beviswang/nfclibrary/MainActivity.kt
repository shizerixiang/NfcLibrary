package com.beviswang.nfclibrary

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.beviswang.nfc.NfcProvider

class MainActivity : AppCompatActivity() {
    private var mNfcProvider: NfcProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mNfcProvider = NfcProvider.getInstance(this)
        if (mNfcProvider?.hasNfcDevice() == false) {
            Log.e("nfc", "Not found nfc device!")
            mNfcProvider = null
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        mNfcProvider?.readNfcData(intent, onResult = { data -> onResult(data) })
    }

    private fun onResult(data: ByteArray?) {
        if (data == null) {
            Toast.makeText(this@MainActivity, "No data!", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this@MainActivity,
                "Data is ${ConvertHelper.byteArray2HexString(data)}", Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        mNfcProvider?.onStart(this)
    }

    override fun onResume() {
        super.onResume()
        mNfcProvider?.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        mNfcProvider?.onPause(this)
    }

    override fun onDestroy() {
        mNfcProvider?.close()
        super.onDestroy()
    }
}
