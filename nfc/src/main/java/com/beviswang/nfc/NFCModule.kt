package com.beviswang.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.nfc.NfcAdapter
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import android.nfc.NdefRecord
import android.nfc.tech.Ndef
import android.util.Log
import java.util.*

/**
 * The class about NFC module.
 * Create by shize on 2018/4/9.
 */
class NFCModule(activity: Activity) : INFCModule {
    private val TAG = javaClass.simpleName
    private var mWeakActivity = WeakReference<Activity>(activity)
    private var mNfcAdapter: NfcAdapter? = null

    override fun hasNFCDevice(): Boolean {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mWeakActivity.get())
        // Check whether the device supports the NFC function.
        if (mNfcAdapter == null) {
            Log.e(TAG,"This device is not support NFC!")
            return false
        }
        // Check the NFC function is opened.
        if (mNfcAdapter?.isEnabled != true) {
            Log.e(TAG,"Please open the NFC function to the system settings!")
            return false
        }
        return true
    }

    override fun readNFCMUTag(intent: Intent): ByteArray? {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (!checkTag(tag)) {
            Log.e(TAG,"This NFC tag is not support MifareUltralight data format.")
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

    override fun writeNFCMUTag(intent: Intent, data: String): Boolean {
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        var result = false
        if (!checkTag(tag)) {
            Log.e(TAG,"This NFC tag is not support MifareUltralight data format.")
            return result
        }
        // TODO Only 48 bytes of data can be written.
        val ultralight = MifareUltralight.get(tag)
        try {
            ultralight.connect()
            // 从第五页开始写，中文需要转换成 GB2312 格式  4-15 为可用页，每页数据为 4 byte
            val dataArray = data.toByteArray(Charset.forName("GB2312"))
            if (dataArray.size > 48) {
                Log.e(TAG,"无法写入全部的数据！最大仅支持 48 字节的数据，将可能出现后段数据丢失！")
            }
            (0 until 12).forEach {
                if (dataArray.size < it * 4 + 3)
                    return@forEach
                val writeData = dataArray.asList().subList(it * 4, it * 4 + 4).toByteArray()
                ultralight.writePage(4 + it, writeData)
            }
            Log.i(TAG,"Write to success!")
            result = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ultralight?.close()
            return result
        }
    }

    override fun readNFCTag(intent: Intent): ByteArray? {
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
                }else {
                    Log.e(TAG,"Message is null value!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }else{
            Log.e(TAG,"Action is not ACTION_NDEF_DISCOVERED!")
        }
        return dataString
    }

    /**
     * 解析NDEF文本数据，从第三个字节开始，后面的文本数据
     * @param ndefRecord NdefRecord.
     * @return Read the string.
     */
    private fun parseTextRecord(ndefRecord: NdefRecord): ByteArray? {
        /**
         * 判断数据是否为NDEF格式
         */
        //判断TNF
        if (ndefRecord.tnf != NdefRecord.TNF_WELL_KNOWN) {
            Log.e(TAG,"TNF is not TNF_WELL_KNOWN!")
            return null
        }
        //判断可变的长度的类型
        if (!Arrays.equals(ndefRecord.type, NdefRecord.RTD_TEXT)) {
            Log.e(TAG,"Type is not RID_TEXT!")
            return null
        }
        try {
            //获得字节数组，然后进行分析
            val payload = ndefRecord.payload
            //下面开始NDEF文本数据第一个字节，状态字节
            //判断文本是基于UTF-8还是UTF-16的，取第一个字节"位与"上16进制的80，16进制的80也就是最高位是1，
            //其他位都是0，所以进行"位与"运算后就会保留最高位
//            val textEncoding = if (payload[0].toInt() and 0x80 == 0) "UTF-8" else "UTF-16"
            //3f最高两位是0，第六位是1，所以进行"位与"运算后获得第六位
            val languageCodeLength = payload[0].toInt() and 0x3f
            //下面开始NDEF文本数据第二个字节，语言编码
            //获得语言编码
            val languageCode = String(payload, 1, languageCodeLength, Charset.forName("US-ASCII"))
            Log.i(TAG,"Language code is $languageCode")
            //下面开始NDEF文本数据后面的字节，解析出文本
            return payload.asList().subList(languageCodeLength + 1,
                    payload.size - languageCodeLength - 1).toByteArray()
        } catch (e: Exception) {
            throw IllegalArgumentException()
        }
    }

    override fun writeNFCTag(intent: Intent, data: String): Boolean {
        //获取Tag对象
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
     * 创建NDEF文本数据
     * @param text
     * @return
     */
    private fun createTextRecord(text: String): NdefRecord {
        val langBytes = Locale.CHINA.language.toByteArray(Charset.forName("US-ASCII"))
        val utfEncoding = Charset.forName("UTF-8")
        //将文本转换为UTF-8格式
        val textBytes = text.toByteArray(utfEncoding)
        //设置状态字节编码最高位数为0
        val utfBit = 0
        //定义状态字节
        val status = (utfBit + langBytes.size).toChar()
        val data = ByteArray(1 + langBytes.size + textBytes.size)
        //设置第一个状态字节，先将状态码转换成字节
        data[0] = status.toByte()
        //设置语言编码，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1到langBytes.length的位置
        System.arraycopy(langBytes, 0, data, 1, langBytes.size)
        //设置文本字节，使用数组拷贝方法，从0开始拷贝到data中，拷贝到data的1 + langBytes.length
        //到textBytes.length的位置
        System.arraycopy(textBytes, 0, data, 1 + langBytes.size, textBytes.size)
        //通过字节传入NdefRecord对象
        //NdefRecord.RTD_TEXT：传入类型 读写
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, ByteArray(0), data)
    }

    override fun close() {
        val activity = mWeakActivity.get()
        mNfcAdapter?.disableForegroundDispatch(activity)
    }
}