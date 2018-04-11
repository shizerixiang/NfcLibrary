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
     * @param onResult Result callback on read data.
     */
    fun readNfcData(intent: Intent, onResult: (ByteArray?) -> Unit)

    /**
     * Write string data to nfc tag.
     * @param intent Intent.
     * @param data Write the card's string data.
     * @param onResult A callback on whether the write data is successful or not.
     */
    fun writeNfcData(intent: Intent, data: String, onResult: ((Boolean) -> Unit)?)

    /**
     * Write byte data to nfc tag.
     * @param intent Intent.
     * @param data Write the card's byte data.
     * @param onResult A callback on whether the write data is successful or not.
     */
    fun writeNfcData(intent: Intent, data: ByteArray, onResult: ((Boolean) -> Unit)?)
}