package com.beviswang.nfclibrary

import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException

import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor

object DES {

    fun gen_sessionKey(b: ByteArray): ByteArray {

        val key = byteArrayOf(0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte(), 0x0.toByte())
        val response = decrypt(key, b)
        var rndB = response
        val rndA = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val rndAB = ByteArray(16)
        System.arraycopy(rndA, 0, rndAB, 0, 8)
        rndB = leftShift(rndB!!)
        rndB = xorBytes(rndA, rndB)
        rndB = decrypt(key, rndB)
        System.arraycopy(rndB!!, 0, rndAB, 8, 8)
        return rndAB
    }

    private fun xorBytes(rndA: ByteArray, rndB: ByteArray): ByteArray {
        // TODO Auto-generated method stub
        val b = ByteArray(rndB.size)
        for (i in rndB.indices) {
            b[i] = (rndA[i] xor rndB[i])
        }
        return b
    }

    fun leftShift(data: ByteArray): ByteArray {
        // TODO Auto-generated method stub
        val temp = ByteArray(data.size)
        temp[data.size - 1] = data[0]
        for (i in 1 until data.size) {
            temp[i - 1] = data[i]
        }
        return temp
    }

    fun decrypt(key: ByteArray, enciphered_data: ByteArray?): ByteArray? {

        try {
            val iv = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            val ivParameterSpec = IvParameterSpec(iv)
            val s = SecretKeySpec(key, "DESede")
            val cipher: Cipher
            cipher = Cipher.getInstance("DESede/CBC/NoPadding", "BC")
            cipher.init(Cipher.DECRYPT_MODE, s, ivParameterSpec)
            return cipher.doFinal(enciphered_data!!)
        } catch (e: NoSuchAlgorithmException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: NoSuchProviderException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: NoSuchPaddingException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: InvalidKeyException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: InvalidAlgorithmParameterException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: IllegalBlockSizeException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: BadPaddingException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }

        return null
    }

}