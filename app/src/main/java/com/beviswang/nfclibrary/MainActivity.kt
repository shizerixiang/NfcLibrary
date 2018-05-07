package com.beviswang.nfclibrary

import android.annotation.SuppressLint
import java.io.IOException

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.MalformedMimeTypeException
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcA
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.TextView
import android.widget.Toast
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

/**
 * Attempt to read 14443-3 Smart card via NFC.
 * Features code from John Philip Bigcas:
 * http://noobstah.blogspot.co.nz/2013/04/mifare-desfire-ev1-and-android.html
 */
class MainActivity : Activity() {

    private var intentFiltersArray: Array<IntentFilter>? = null
    private var techListsArray: Array<Array<String>>? = null
    private var mAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var mTransBytes: TextView? = null
    private var mDecId: TextView? = null
    private var mHexId: TextView? = null
    private var mInfo: TextView? = null
    private var mNfc: NfcA? = null
    private var mStringBuilder: StringBuilder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAdapter = NfcAdapter.getDefaultAdapter(this)

        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this,
                javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("*/*")
        } catch (e: MalformedMimeTypeException) {
            throw RuntimeException()
        }

        intentFiltersArray = arrayOf(ndef)
        techListsArray = arrayOf(arrayOf(NfcA::class.java.name))

        // Initialise TextView fields
        mHexId = findViewById<View>(R.id.hexId) as TextView?
        mDecId = findViewById<View>(R.id.decId) as TextView?
        mTransBytes = findViewById<View>(R.id.transceivableBytes) as TextView?
        mInfo = findViewById<View>(R.id.infoView) as TextView?

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun processIntent(intent: Intent) {
        doAsync {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            mNfc = NfcA.get(tag)
            try {
                mNfc!!.connect()
                Log.v("tag", "connected.")
                val id = mNfc!!.tag.id
                Log.v("tag", "Got id from tag:$id")
                uiThread {
                    mHexId!!.text = getHex(id)
                    mDecId!!.text = getDec(id)
                    mTransBytes!!.text = "" + mNfc!!.maxTransceiveLength
                }

                val response = mNfc!!.transceive(NATIVE_SELECT_COMMAND)
                displayText("Select App", getHex(response))
                authenticate()
                val read = readCommand()
                displayText("Read", read)

            } catch (e: IOException) {
                // TODO: handle exception
                e.printStackTrace()
            } finally {
                if (mNfc != null) {
                    try {
                        mNfc!!.close()
                    } catch (e: IOException) {
                        Log.v("tag", "error closing the tag")
                    }
                }
            }
        }
    }

    private fun readCommand(): String {
        val fileNo = 0x01.toByte()
        val offset = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val length = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        val message = ByteArray(8)
        message[0] = READ_DATA_COMMAND
        message[1] = fileNo

        System.arraycopy(offset, 0, message, 2, 3)
        System.arraycopy(length, 0, message, 2, 3)

        val response: ByteArray
        try {
            response = mNfc!!.transceive(message)
            Toast.makeText(this, "Response Length = " + response.size, Toast.LENGTH_LONG).show()
            return getHex(response)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        return "Read failed"
    }

    private fun authenticate() {
        // TODO Auto-generated method stub
        val rndB = ByteArray(8)
        var response: ByteArray
        try {
            response = mNfc!!.transceive(NATIVE_AUTHENTICATION_COMMAND)
            System.arraycopy(response, 1, rndB, 0, 8)

            val command = ByteArray(17)

            System.arraycopy(DES.gen_sessionKey(rndB), 0, command, 1, 16)
            command[0] = 0xAF.toByte()

            response = mNfc!!.transceive(command)
            displayText("Authentication Status", getHex(response))
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

    }

    private fun getDec(bytes: ByteArray): String {
        var result: Long = 0
        var factor: Long = 1
        for (i in bytes.indices) {
            val value = bytes[i].toInt() and 0xff
            result += value * factor
            factor *= 256L
        }
        return result.toString() + ""
    }

    private fun getHex(bytes: ByteArray): String {
        Log.v("tag", "Getting hex")
        val sb = StringBuilder()
        for (i in bytes.indices.reversed()) {
            val b = bytes[i].toInt() and 0xff
            if (b < 0x10)
                sb.append('0')
            sb.append(Integer.toHexString(b))
            if (i > 0) {
                sb.append(" ")
            }
        }
        return sb.toString()
    }

    private fun displayText(label: String?, text: String) {
        if (mStringBuilder == null) {
            mStringBuilder = StringBuilder()
        }
        if (label != null) {
            mStringBuilder!!.append(label)
            mStringBuilder!!.append(":")
        }
        mStringBuilder!!.append(text)
        mStringBuilder!!.append("\n")

        mInfo!!.text = mStringBuilder!!.toString()
    }

    public override fun onPause() {
        super.onPause()
        mAdapter!!.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        mAdapter!!.enableForegroundDispatch(this, pendingIntent,
                intentFiltersArray, techListsArray)
    }

    public override fun onNewIntent(intent: Intent) {
        Log.v("tag", "In onNewIntent")
        processIntent(intent)
    }

    companion object {

        // Desfire commands
        private val SELECT_COMMAND = 0x5A.toByte()
        private val AUTHENTICATE_COMMAND = 0x0A.toByte()
        private val READ_DATA_COMMAND = 0xBD.toByte()
        private val NATIVE_AUTHENTICATION_COMMAND = byteArrayOf(0x0A.toByte(), 0x00.toByte())
        private val NATIVE_SELECT_COMMAND = byteArrayOf(0x5A.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
    }

}
