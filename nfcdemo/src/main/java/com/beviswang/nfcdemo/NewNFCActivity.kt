package com.beviswang.nfcdemo

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.beviswang.nfcdemo.nfcdemo.DataUtils
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

class NewNFCActivity : AppCompatActivity() {
    private val TAG = "gh0st"
    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null
    private lateinit var tv: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_nfc)
        tv = findViewById(R.id.tv)
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)
        mPendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass), 0)
    }

    override fun onPause() {
        super.onPause()
        if (mNfcAdapter != null) {
            mNfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    override fun onResume() {
        super.onResume()
        if (mNfcAdapter != null) {
            mNfcAdapter!!.enableForegroundDispatch(this, mPendingIntent, null, null)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
//        print("检测到了")
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            print(Arrays.toString(tag.techList))
            tag.techList.forEach {
                print("设备支持：$it id:${DataUtils.saveHex2String(tag.id)}")
                when (it) {
                    MifareClassic::class.java.name -> readMifareClassic(tag)
                    MifareUltralight::class.java.name -> readMifareUltralight(tag)
                    NfcA::class.java.name -> readNfcA(tag)
                    NfcB::class.java.name -> readNfcB(tag)
                    NfcF::class.java.name -> readNfcF(tag)
                    NfcV::class.java.name -> readNfcV(tag)
                    Ndef::class.java.name -> readNdeF(tag)
                    NfcBarcode::class.java.name -> readNfcBarcode(tag)
                    IsoDep::class.java.name -> readIsoDep(tag)
                }
            }
        }
        val rawArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        if (rawArray != null) {
            val mNdefMsg = rawArray[0] as NdefMessage
            val mNdefRecord = mNdefMsg.records[0]
            if (mNdefRecord != null) {
                print(String(mNdefRecord.payload, Charset.forName("UTF-8")))
            }
        } else {
            print("ops!!")
        }
    }

    private fun readNfcA(tag: Tag) {
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            print(DataUtils.saveHex2String(nfcA.atqa))
        }
    }

    private fun readNfcB(tag: Tag) {}
    private fun readNfcF(tag: Tag) {}
    private fun readNfcV(tag: Tag) {}
    private fun readNdeF(tag: Tag) {}
    private fun readNfcBarcode(tag: Tag) {}
    /**
     * 支持深圳通
     */
    private fun getSelectCommand(aid: ByteArray): ByteArray {
        val cmd_pse = ByteBuffer.allocate(aid.size + 6)
        cmd_pse.put(0x00.toByte()) // CLA Class
                .put(0xA4.toByte()) // INS Instruction
                .put(0x04.toByte()) // P1 Parameter 1
                .put(0x00.toByte()) // P2 Parameter 2
                .put(aid.size.toByte()) // Lc
                .put(aid).put(0x00.toByte()) // Le
        return cmd_pse.array()
    }

    private fun readIsoDep(tag: Tag) {
        val isodep = IsoDep.get(tag)
        if (isodep != null) {
            isodep.connect()
            val mf = byteArrayOf('1'.toByte(), 'P'.toByte(), 'A'.toByte(), 'Y'.toByte(),
                    '.'.toByte(), 'S'.toByte(), 'Y'.toByte(), 'S'.toByte(), '.'.toByte(), 'D'.toByte(),
                    'D'.toByte(), 'F'.toByte(), '0'.toByte(), '1'.toByte())
            val mfRsp = isodep.transceive(getSelectCommand(mf))
            print("mfRsp:" + DataUtils.saveHex2String(mfRsp))
            //select Main Application
            val szt = byteArrayOf('P'.toByte(), 'A'.toByte(), 'Y'.toByte(), '.'.toByte(), 'S'.toByte(), 'Z'.toByte(), 'T'.toByte())
            val sztRsp = isodep.transceive(getSelectCommand(szt))
            print("sztRsp:" + DataUtils.saveHex2String(sztRsp))

            val balance = byteArrayOf(0x80.toByte(), 0x5C.toByte(), 0x00, 0x02, 0x04)
            val balanceRsp = isodep.transceive(balance)
            print("balanceRsp:" + DataUtils.saveHex2String(balanceRsp))
            if (balanceRsp != null && balanceRsp.size > 4) {
                val cash = DataUtils.byteToInt(balanceRsp, 4)
                val ba = cash / 100.0f
                print(ba)
            }
        }
    }

    /**
     * https://blog.csdn.net/lovoo/article/details/52357148
     * 支持读写
     */
    private fun readMifareUltralight(tag: Tag) {
        val light = MifareUltralight.get(tag)
        if (light != null) {
            try {
                light.connect()
                val bytes = light.readPages(4)
                print(DataUtils.saveHex2String(bytes))
            } catch (e: Exception) {
                e.message!!
            }
        }
    }

    private fun readMifareClassic(tag: Tag) {
        try {
            val mfc = MifareClassic.get(tag)
            if (mfc != null) {
                val password = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                mfc.writeBlock(7, password)
                var auth = false
                var metaInfo = ""
                mfc.connect()
                val type = mfc.type//获取TAG的类型
                val sectorCount = mfc.sectorCount//获取TAG中包含的扇区数
                var typeS = ""
                when (type) {
                    MifareClassic.TYPE_CLASSIC -> typeS = "TYPE_CLASSIC"
                    MifareClassic.TYPE_PLUS -> typeS = "TYPE_PLUS"
                    MifareClassic.TYPE_PRO -> typeS = "TYPE_PRO"
                    MifareClassic.TYPE_UNKNOWN -> typeS = "TYPE_UNKNOWN"
                }
                metaInfo += ("卡片类型：" + typeS + "\n共" + sectorCount + "个扇区\n共" + mfc.blockCount + "个块\n存储空间: " + mfc.size + "B\n")
                for (j in 0 until sectorCount) {
                    auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT)
                    val bCount: Int
                    var bIndex: Int
                    if (auth) {
                        metaInfo += "Sector $j:验证成功\n"
                        // 读取扇区中的块
                        bCount = mfc.getBlockCountInSector(j)
                        bIndex = mfc.sectorToBlock(j)
                        for (i in 0 until bCount) {
                            val data = mfc.readBlock(bIndex)
                            metaInfo += ("Block " + bIndex + " : " + DataUtils.saveHex2String(data) + "\n")
                            bIndex++
                        }
                    } else {
                        metaInfo += "Sector $j:验证失败\n"
                    }
                }
                print("nfc 数据:$metaInfo")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun print(obj: Any) {
        Log.e("gh0st", obj.toString())
        runOnUiThread { tv.append("$obj \n") }
    }
}
