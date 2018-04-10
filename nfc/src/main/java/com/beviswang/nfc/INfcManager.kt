package com.beviswang.nfc

import android.content.Intent

/**
 * Data manager interface for NFC.
 * @author BevisWang
 * Create by 2018/4/10.
 */
interface INfcManager {
    /**
     * Read nfc data for nfc tag.
     * @param intent Intent.
     * @return The array of bytes read from the nfc tag.
     */
    fun readNfcData(intent: Intent):ByteArray?

    /**
     * Write string data to nfc tag.
     * @param intent Intent.
     * @param data Write the card's string data.
     * @return Is write to succeed.
     */
    fun writeNfcData(intent: Intent,data:String):Boolean

    /**
     * Write byte data to nfc tag.
     * @param intent Intent.
     * @param data Write the card's byte data.
     * @return Is write to succeed.
     */
    fun writeNfcData(intent: Intent,data:ByteArray):Boolean
}