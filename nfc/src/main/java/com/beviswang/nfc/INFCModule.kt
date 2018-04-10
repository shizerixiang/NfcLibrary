package com.beviswang.nfc

import android.content.Intent
import java.io.Closeable

interface INFCModule:Closeable {
    /**
     * @return Has NFC device.
     */
    fun hasNFCDevice(): Boolean

    /**
     * @param intent NFC tag.
     * @return Read MifareUltralight data from NFC tag.
     */
    fun readNFCMUTag(intent: Intent): ByteArray?

    /**
     * Write data to NFC tag.
     * @param intent tag
     */
    fun writeNFCMUTag(intent: Intent, data: String): Boolean

    /**
     * @param intent Intent.
     * @return Read NdefMessage data from NFC tag.
     */
    fun readNFCTag(intent:Intent): ByteArray?

    /**
     * Write NdefMessage to NFC tag.
     * @param intent NdefMessage.
     * @param data NFC tag.
     */
    fun writeNFCTag(intent: Intent, data: String): Boolean
}