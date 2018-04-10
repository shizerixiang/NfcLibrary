package com.beviswang.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.util.Log
import java.io.Closeable
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.util.*

/**
 * The nfc provider.
 * @author BevisWang
 * Create by 2018/4/10.
 */
class NfcProvider(activity: Activity) : INfcManager, Closeable {
    /** his member is an instance object of Nfc tag. */
    private var mNfcTag: INfcManager? = null
    private var mWeakActivity = WeakReference<Activity?>(activity)
    private var mNfcAdapter: NfcAdapter? = null

    /**
     * Check whether the device has a nfc module.
     * @return Whether there is a module.
     */
    fun hasNfcDevice(): Boolean {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mWeakActivity.get())
        // Check whether the device supports the NFC function.
        if (mNfcAdapter == null) {
            Log.e(TAG, "This device is not support NFC!")
            return false
        }
        // Check the NFC function is opened.
        if (mNfcAdapter?.isEnabled != true) {
            Log.e(TAG, "Please open the NFC function to the system settings!")
            return false
        }
        return true
    }

    /**
     * Sort management Nfc tags.
     * @param intent Intent.
     */
    private fun sortNfcTag(intent: Intent) {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val techList = tag.techList
        techList.forEach {
            Log.e(TAG, "NFC tag type=$it")
        }
    }

    override fun readNfcData(intent: Intent): ByteArray? {
        sortNfcTag(intent)
        return mNfcTag?.readNfcData(intent)
    }

    override fun writeNfcData(intent: Intent, data: String): Boolean {
        sortNfcTag(intent)
        return mNfcTag?.writeNfcData(intent, data) ?: false
    }

    override fun writeNfcData(intent: Intent, data: ByteArray): Boolean {
        sortNfcTag(intent)
        return mNfcTag?.writeNfcData(intent, data) ?: false
    }

    override fun close() {
        mNfcAdapter?.disableForegroundDispatch(mWeakActivity.get())
    }

    /** The NDEF nfc tag. */
    private class NDEFTag : INfcManager {
        override fun readNfcData(intent: Intent): ByteArray? {
            var dataString: ByteArray? = null
            if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
                val rawMsg = intent.getParcelableArrayExtra(
                        NfcAdapter.EXTRA_NDEF_MESSAGES)
                var msg: Array<NdefMessage?>? = null
                var contentSize = 0
                if (rawMsg != null) {
                    msg = arrayOfNulls(rawMsg.size)
                    for (i in rawMsg.indices) {
                        msg[i] = rawMsg[i] as NdefMessage
                        contentSize += msg[i]?.toByteArray()?.size ?: 0
                    }
                }
                try {
                    if (msg != null) {
                        val record = msg[0]!!.records[0]
                        dataString = parseTextRecord(record)
                    } else {
                        Log.e(TAG, "Message is null value!")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Log.e(TAG, "Action is not ACTION_NDEF_DISCOVERED!")
            }
            return dataString
        }

        /**
         * Parsing NDEF text data, starting from third bytes, reading the text data behind.
         * @param ndefRecord NdefRecord.
         * @return Read the string.
         */
        private fun parseTextRecord(ndefRecord: NdefRecord): ByteArray? {
            // Check the INF.
            if (ndefRecord.tnf != NdefRecord.TNF_WELL_KNOWN) {
                Log.e(TAG, "TNF is not TNF_WELL_KNOWN!")
                return null
            }
            // Check the NDEF record type.
            if (!Arrays.equals(ndefRecord.type, NdefRecord.RTD_TEXT)) {
                Log.e(TAG, "Type is not RID_TEXT!")
                return null
            }
            try {
                // Get the bytes.
                val payload = ndefRecord.payload
                val languageCodeLength = payload[0].toInt() and 0x3f
                // Get language code.
                val languageCode = String(payload, 1, languageCodeLength, Charset.forName("US-ASCII"))
                Log.i(TAG, "Language code is $languageCode")
                // Parsing NDEF text data.
                return payload.asList().subList(languageCodeLength + 1,
                        payload.size - languageCodeLength - 1).toByteArray()
            } catch (e: Exception) {
                throw IllegalArgumentException()
            }
        }

        override fun writeNfcData(intent: Intent, data: String): Boolean {
            return writeNfcData(intent, data.toByteArray(Charset.defaultCharset()))
        }

        override fun writeNfcData(intent: Intent, data: ByteArray): Boolean {
            // Get the nfc tag.
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val ndefMessage = NdefMessage(
                    arrayOf(createTextRecord(data)))
            var result = false
            try {
                val ndef = Ndef.get(tag)
                ndef.connect()
                ndef.writeNdefMessage(ndefMessage)
                result = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                return result
            }
        }

        /**
         * Create NDEF bytes data.
         * @param bytes String data.
         * @return NdefRecord.
         */
        private fun createTextRecord(bytes: ByteArray): NdefRecord {
            val langBytes = Locale.CHINA.language.toByteArray(Charset.forName("US-ASCII"))
            // 将文本转换为UTF-8格式
            // 设置状态字节编码最高位数为0
            val utfBit = 0
            // 定义状态字节
            val status = (utfBit + langBytes.size).toChar()
            val data = ByteArray(1 + langBytes.size + bytes.size)
            // 设置第一个状态字节，先将状态码转换成字节
            data[0] = status.toByte()
            // 设置语言编码，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1到langBytes.length的位置
            System.arraycopy(langBytes, 0, data, 1, langBytes.size)
            // 设置文本字节，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1 + langBytes.length
            // 到textBytes.length的位置
            System.arraycopy(bytes, 0, data, 1 + langBytes.size, bytes.size)
            // 通过字节传入NdefRecord对象
            // NdefRecord.RTD_TEXT：传入类型 读写
            return NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                    NdefRecord.RTD_TEXT, ByteArray(0), data)
        }
    }

    /** The MifareUltralight nfc tag. */
    private class MifareUltralightTag : INfcManager {
        override fun readNfcData(intent: Intent): ByteArray? {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (!checkTag(tag)) {
                Log.e(TAG, "This NFC tag is not support MifareUltralight data format.")
                return null
            }
            var data: ByteArray? = null

            val ultralight = MifareUltralight.get(tag)
            try {
                ultralight?.connect()
                val dataArray = ultralight.readPages(4)
                data = dataArray
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ultralight?.close()
            }
            return data
        }

        /**
         * Check NFC tag is support MifareUltralight data format.
         *
         * @param tag NFC tag.
         * @return Is support MifareUltralight data format.
         */
        private fun checkTag(tag: Tag): Boolean {
            val techList = tag.techList
            var haveMifareUltralight = false
            for (tech in techList) {
                if (tech.indexOf("MifareUltralight") >= 0) {
                    haveMifareUltralight = true
                    break
                }
            }
            return haveMifareUltralight
        }

        override fun writeNfcData(intent: Intent, data: String): Boolean {
            return writeNfcData(intent, data.toByteArray(Charset.forName("GBK")))
        }

        override fun writeNfcData(intent: Intent, data: ByteArray): Boolean {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            var result = false
            if (!checkTag(tag)) {
                Log.e(TAG, "This NFC tag is not support MifareUltralight data format.")
                return result
            }
            // Only 48 bytes of data can be written.
            val ultralight = MifareUltralight.get(tag)
            try {
                ultralight.connect()
                // Starting from page fifth, Chinese needs to be converted to GBK format.
                // 4-15 is available page, and the data of each page is 4 byte.
                if (data.size > 48) {
                    Log.e(TAG, "Can not write data, because the max data is 48 bytes.")
                }
                (0 until 12).forEach {
                    if (data.size < it * 4 + 3)
                        return@forEach
                    val writeData = data.asList().subList(it * 4, it * 4 + 4).toByteArray()
                    ultralight.writePage(4 + it, writeData)
                }
                Log.i(TAG, "Write to success!")
                result = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ultralight?.close()
                return result
            }
        }
    }

    /** The NfcA nfc tag. */
    private class NfcATag : INfcManager {
        override fun readNfcData(intent: Intent): ByteArray? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun writeNfcData(intent: Intent, data: String): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun writeNfcData(intent: Intent, data: ByteArray): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    /** The NfcB nfc tag. */
    private class NfcBTag : INfcManager {
        override fun readNfcData(intent: Intent): ByteArray? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun writeNfcData(intent: Intent, data: String): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun writeNfcData(intent: Intent, data: ByteArray): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    /** The IsoDep nfc tag. */
    private class IsoDepTag : INfcManager {
        override fun readNfcData(intent: Intent): ByteArray? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun writeNfcData(intent: Intent, data: String): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun writeNfcData(intent: Intent, data: ByteArray): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    companion object {
        private const val TAG = "NfcProvider"
    }
}