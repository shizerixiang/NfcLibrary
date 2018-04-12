package com.beviswang.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.util.Log
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.Closeable
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.util.*
import android.nfc.tech.MifareClassic
import java.io.IOException

/**
 * The nfc provider.
 * @author BevisWang
 * Create by 2018/4/10.
 */
class NfcProvider(activity: Activity) : INfcManager, Closeable {
    /** his member is an instance object of Nfc tag. */
    private var mWeakActivity = WeakReference<Activity?>(activity)
    private var mNfcTag: INfcManager? = null
    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null
    private var isCustomNfcTagManager: Boolean = false

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
     * Set the nfc tag manager.
     * @param nfcTag Nfc tag manager.
     */
    fun setNfcTag(nfcTag: INfcManager) {
        isCustomNfcTagManager = true
        mNfcTag = nfcTag
    }

    /** When activity onStart. */
    fun onStart(activity: Activity) {
        mPendingIntent = PendingIntent.getActivity(activity, 0,
                Intent(mWeakActivity.get(), activity.javaClass), 0)
    }

    /** When activity onResume. */
    fun onResume(activity: Activity) {
        mNfcAdapter?.enableForegroundDispatch(activity, mPendingIntent, null, null)
    }

    /** When activity onPause. */
    fun onPause(activity: Activity) {
        mNfcAdapter?.disableForegroundDispatch(activity)
    }

    /**
     * Sort management Nfc tags.
     * @param intent Intent.
     */
    private fun sortNfcTag(intent: Intent) {
        if (isCustomNfcTagManager) return
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        val techList = tag.techList
        techList.forEach { Log.e(TAG, "Tech ->->-> $it") }
        // Sort nfc tag.
        mNfcTag = when (techList[0]) {
            NDEF -> NDEFTag()
            MIFARE_ULTRALIGHT -> MifareUltralightTag()
            MIFARE_CLASSIC -> MifareClassicTag()
            ISO_DEP -> IsoDepTag()
            NFC_A -> NfcATag()
            else -> throw Exception("Unknown label type!")
        }
    }

    override fun readNfcData(intent: Intent, onResult: (ByteArray?) -> Unit) {
        sortNfcTag(intent)
        mNfcTag?.readNfcData(intent, onResult)
    }

    override fun writeNfcData(intent: Intent, data: String, onResult: ((Boolean) -> Unit)?) {
        sortNfcTag(intent)
        mNfcTag?.writeNfcData(intent, data, onResult)
    }

    override fun writeNfcData(intent: Intent, data: ByteArray, onResult: ((Boolean) -> Unit)?) {
        sortNfcTag(intent)
        mNfcTag?.writeNfcData(intent, data, onResult)
    }

    override fun close() {
        mNfcAdapter = null
        INSTANCE = null
    }

    /** The NDEF nfc tag. */
    class NDEFTag : INfcManager {

        override fun readNfcData(intent: Intent, onResult: (ByteArray?) -> Unit) {
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
            onResult(dataString)
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

        override fun writeNfcData(intent: Intent, data: String, onResult: ((Boolean) -> Unit)?) {
            writeNfcData(intent, data.toByteArray(Charset.defaultCharset()), onResult)
        }

        override fun writeNfcData(intent: Intent, data: ByteArray, onResult: ((Boolean) -> Unit)?) {
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
                onResult?.invoke(result)
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

    /** The MifareUltralight nfc tag. Is I/O operations on a Tag. */
    class MifareUltralightTag : INfcManager {

        override fun readNfcData(intent: Intent, onResult: (ByteArray?) -> Unit) {
            doAsync {
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                if (!checkTag(tag)) {
                    Log.e(TAG, "This NFC tag is not support MifareUltralight data format.")
                    uiThread { onResult(null) }
                    return@doAsync
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
                uiThread { onResult(data) }
            }
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

        override fun writeNfcData(intent: Intent, data: String, onResult: ((Boolean) -> Unit)?) {
            writeNfcData(intent, data.toByteArray(Charset.forName("GBK")), onResult)
        }

        override fun writeNfcData(intent: Intent, data: ByteArray, onResult: ((Boolean) -> Unit)?) {
            doAsync {
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                var result = false
                if (!checkTag(tag)) {
                    Log.e(TAG, "This NFC tag is not support MifareUltralight data format.")
                    uiThread { onResult?.invoke(result) }
                    return@doAsync
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
                    uiThread { onResult?.invoke(result) }
                }
            }
        }
    }

    /** The MifareClassicTag nfc tag. Is I/O operations on a Tag. */
    class MifareClassicTag : INfcManager {
        override fun readNfcData(intent: Intent, onResult: (ByteArray?) -> Unit) {
            doAsync {
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                val mfc = MifareClassic.get(tag)
                for (tech in tag.techList) {
                    Log.e(TAG, tech)
                }
                var auth: Boolean
                var metaInfo = ""
                // Read TAG
                try {
                    //Enable I/O operations to the tag from this TagTechnology object.
                    mfc.connect()
                    val type = mfc.type // Obtain tag type.
                    val sectorCount = mfc.sectorCount // Obtain tag sector count.
                    var typeS = ""
                    when (type) {
                        MifareClassic.TYPE_CLASSIC -> typeS = "TYPE_CLASSIC"
                        MifareClassic.TYPE_PLUS -> typeS = "TYPE_PLUS"
                        MifareClassic.TYPE_PRO -> typeS = "TYPE_PRO"
                        MifareClassic.TYPE_UNKNOWN -> typeS = "TYPE_UNKNOWN"
                    }
                    Log.i(TAG, "CardType: " + typeS + "\nSectorCount: " + sectorCount + "\nBlockCount: "
                            + mfc.blockCount + "\nStorageSpace: " + mfc.size + "B\n")
                    for (j in 0 until sectorCount) {
                        //Authenticate a sector with key A.
                        auth = mfc.authenticateSectorWithKeyA(j,
                                MifareClassic.KEY_NFC_FORUM)
                        val bCount: Int
                        var bIndex: Int
                        if (auth) {
                            Log.e(TAG, "Sector $j: verification succeed\n")
                            // 读取扇区中的块
                            bCount = mfc.getBlockCountInSector(j)
                            bIndex = mfc.sectorToBlock(j)
                            for (i in 0 until bCount) {
                                val data = mfc.readBlock(bIndex)
                                metaInfo += ConvertHelper.ByteArrayToHexString(data)
                                bIndex++
                            }
                        } else {
                            Log.e(TAG, "Sector $j: verification failed\n")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (mfc != null) {
                        try {
                            mfc.close()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } finally {
                            uiThread { onResult(ConvertHelper.HexStringToByteArray(metaInfo)) }
                        }
                    } else {
                        uiThread { onResult(null) }
                    }
                }
            }
        }

        override fun writeNfcData(intent: Intent, data: String, onResult: ((Boolean) -> Unit)?) {
            writeNfcData(intent, data.toByteArray(), onResult)
        }

        override fun writeNfcData(intent: Intent, data: ByteArray, onResult: ((Boolean) -> Unit)?) {
            doAsync {
                var isSucceed = false
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                val mfc = MifareClassic.get(tag)
                try {
                    mfc.connect()
                    val sectorAddress: Short = 1
                    val auth = mfc.authenticateSectorWithKeyA(sectorAddress.toInt(),
                            MifareClassic.KEY_NFC_FORUM)
                    if (auth) {
                        // the last block of the sector is used for KeyA and KeyB cannot be overwritted
                        mfc.writeBlock(4, data)
                        mfc.close()
                        isSucceed = true
                    }
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                } finally {
                    try {
                        mfc.close()
                    } catch (e: IOException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    } finally {
                        uiThread { onResult?.invoke(isSucceed) }
                    }
                }
            }
        }
    }

    /** The NfcA nfc tag. Is I/O operations on a Tag. */
    class NfcATag : INfcManager {
        override fun readNfcData(intent: Intent, onResult: (ByteArray?) -> Unit) {
            doAsync {
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                var resultBytes: ByteArray? = null
                val nfcA = NfcA.get(tag)
                try {
                    nfcA.connect()
                    if (!nfcA.isConnected) return@doAsync
                    val readCmd = byteArrayOf(0x30, 0x05) // NTAG216
                    resultBytes = nfcA.transceive(readCmd)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    nfcA.close()
                    uiThread { onResult(resultBytes) }
                }
            }
        }

        override fun writeNfcData(intent: Intent, data: String, onResult: ((Boolean) -> Unit)?) {
            // TODO write nfc tag data.
        }

        override fun writeNfcData(intent: Intent, data: ByteArray, onResult: ((Boolean) -> Unit)?) {
            // TODO write nfc tag data.
        }
    }

    /** The IsoDep nfc tag. Is I/O operations on a Tag. */
    class IsoDepTag : INfcManager {
        override fun readNfcData(intent: Intent, onResult: (ByteArray?) -> Unit) {
            doAsync {
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                var resultBytes: ByteArray? = null
                val isoDep = IsoDep.get(tag)
                try {
                    isoDep.connect()
                    if (!isoDep.isConnected) return@doAsync
                    val requestDir = "00A4040005${ConvertHelper.ByteArrayToHexString("2PAY.SYS.DDF01".toByteArray())}00"
                    Log.e(TAG, "Request bytes:$requestDir")
                    // Choose card dir.
                    val respDir = isoDep.transceive(ConvertHelper.HexStringToByteArray(requestDir))
                    Log.e(TAG, "Response bytes:${ConvertHelper.ByteArrayToHexString(respDir)}")
                    // Read card balance.
                    resultBytes = isoDep.transceive(ConvertHelper.HexStringToByteArray("805C000204"))
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isoDep.close()
                    uiThread { onResult(resultBytes) }
                }
            }
        }

        override fun writeNfcData(intent: Intent, data: String, onResult: ((Boolean) -> Unit)?) {
            // TODO write nfc tag data.
        }

        override fun writeNfcData(intent: Intent, data: ByteArray, onResult: ((Boolean) -> Unit)?) {
            // TODO write nfc tag data.
        }
    }

    companion object {
        private const val TAG = "NfcProvider"
        private const val NFC_A = "android.nfc.tech.NfcA"
        private const val NFC_B = "android.nfc.tech.NfcB"
        private const val NFC_F = "android.nfc.tech.NfcF"
        private const val NFC_V = "android.nfc.tech.NfcV"
        private const val NDEF = "android.nfc.tech.Ndef"
        private const val ISO_DEP = "android.nfc.tech.IsoDep"
        private const val NDEF_FORMATABLE = "android.nfc.tech.NdefFormatable"
        private const val MIFARE_CLASSIC = "android.nfc.tech.MifareClassic"
        private const val MIFARE_ULTRALIGHT = "android.nfc.tech.MifareUltralight"

        private var INSTANCE: NfcProvider? = null

        fun getInstance(activity: Activity): NfcProvider {
            if (INSTANCE == null) INSTANCE = NfcProvider(activity)
            return INSTANCE!!
        }
    }
}