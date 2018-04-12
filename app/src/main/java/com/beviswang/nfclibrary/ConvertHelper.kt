package com.beviswang.nfclibrary

import java.nio.charset.Charset

/**
 * 转换工具类
 * 示例如下：
 *            val sp2 = SerialPortModule.getSerialPort("/dev/ttyMT2", 9600, 0)
 *            val ba2 = ConvertHelper.string2ByteArray("55AA001103000000201701041544552455AA")
 *            Log.i(javaClass.simpleName, ConvertHelper.byteArray2HexString(ba2))
 *            sp2.writePort(ba2)
 *            sp2.readPort(object : SerialPortModule.OnResultListener {
 *                override fun onReceived(data: ByteArray) {
 *                    Log.i(javaClass.simpleName, "数据：" + ConvertHelper.byteArray2HexString(data))
 *                }
 *            })
 * Created by shize on 2018/1/9.
 */
object ConvertHelper {
    /**
     * byte 数组转换为显示的字符串
     *
     * @param byteArray byte 数组
     */
    fun byteArray2HexString(byteArray: ByteArray): String {
        var strBa = ""
        byteArray.forEach {
            var s = Integer.toHexString(it.toInt() and 0xFF)
            if (s.length < 2) s = "0" + s
            strBa += s
        }
        return strBa
    }

    /**
     * Convert hex string to byteArray
     * @param hexString the hex string
     * @return byteArray
     */
    fun string2ByteArray(hexString: String): ByteArray {
        var hexStr = hexString
        hexStr = hexStr.toUpperCase() // 转大写字母
        hexStr = hexStr.trim() // 去除空格
        val length = hexStr.length / 2 // 计算 byte 数组位数
        val hexChars = hexStr.toCharArray()
        val d = ByteArray(length)
        for (i in 0 until length) {
            val pos = i * 2
            d[i] = (char2Byte(hexChars[pos]).toInt() shl 4 or char2Byte(hexChars[pos + 1]).toInt()).toByte()
            // 转换为无符号的字节
//            d[i] = (d[i].toInt() and 0xff).toByte()
        }
        return d
    }

    /**
     * Convert char to byte
     * @param c char
     * @return byte
     */
    private fun char2Byte(c: Char): Byte {
        return "0123456789ABCDEF".indexOf(c).toByte()
    }

    /**
     * 二进制转换为十六进制
     * 十六进制字符串默认会在最左边补 0
     *
     * @param bString 二进制字符串
     * @return 转换后的十六进制字符串
     */
    fun binaryString2hexString(bString: String?): String? {
        if (bString == null || bString == "" || bString.length % 8 != 0)
            return null
        val tmp = StringBuffer()
        var iTmp = 0
        var i = 0
        while (i < bString.length) {
            iTmp = (0..3).sumBy { Integer.parseInt(bString.substring(i + it, i + it + 1)) shl 4 - it - 1 }
            tmp.append(Integer.toHexString(iTmp))
            i += 4
        }
        return tmp.toString()
    }

    /**
     * 十六进制转换为二进制字符串
     * 二进制字符串默认会在最左边补 0
     *
     * @param hexString 十六进制字符串
     * @return 转换后的二进制字符串
     */
    fun hexString2binaryString(hexString: String?): String? {
        if (hexString == null || hexString.length % 2 != 0)
            return null
        var bString = ""
        var tmp: String
        for (i in 0 until hexString.length) {
            tmp = "0000" + Integer.toBinaryString(Integer.parseInt(hexString
                    .substring(i, i + 1), 16))
            bString += tmp.substring(tmp.length - 4)
        }
        return bString
    }

    /**
     * 将整形转换为十六进制字符串
     *
     * @param integer 整形
     * @return 十六进制字符串
     */
    fun integer2hexString(integer: Int): String {
        return Integer.toHexString(integer)
    }

    /**
     * @param f float 类型数据
     * @return 将 float 转换为十六进制字符串
     */
    fun float2HexString(f: Float): String {
        return byteArray2HexString(float2ByteArray(f))
    }

    /**
     * 浮点转换为字节
     *
     * @param f
     * @return
     */
    fun float2ByteArray(f: Float): ByteArray {
        // 把float转换为byte[]
        val fBit = java.lang.Float.floatToIntBits(f)
        val b = ByteArray(4)
        for (i in 0..3) {
            b[i] = (fBit shr 24 - i * 8).toByte()
        }
        // 翻转数组
        val len = b.size
        // 建立一个与源数组元素类型相同的数组
        val dest = ByteArray(len)
        // 为了防止修改源数组，将源数组拷贝一份副本
        System.arraycopy(b, 0, dest, 0, len)
        var temp: Byte
        // 将顺位第i个与倒数第i个交换
        for (i in 0 until len / 2) {
            temp = dest[i]
            dest[i] = dest[len - i - 1]
            dest[len - i - 1] = temp
        }
        return dest
    }

    /**
     * 字节转换为浮点
     *
     * @param b 字节（至少4个字节）
     * @return
     */
    fun byteArray2Float(b: ByteArray): Float {
        var l: Int = b[0].toInt()
        l = l and 0xff
        l = l or (b[1].toLong() shl 8).toInt()
        l = l and 0xffff
        l = l or (b[2].toLong() shl 16).toInt()
        l = l and 0xffffff
        l = l or (b[3].toLong() shl 24).toInt()
        return java.lang.Float.intBitsToFloat(l)
    }

    /**
     * 将十六进制字符串转换为整形
     *
     * @param hexString 十六进制字符串
     * @return 整形
     */
    fun hexString2integer(hexString: String): Int {
        return Integer.parseInt(hexString, 16)
    }

    /**
     * 将 GBK 编码格式的字符串转换为 16 进制字符串
     *
     * @param s 字符串
     * @return 16 进制字符串
     */
    fun stringGBK2HexString(s: String): String {
        return ConvertHelper.byteArray2HexString(s.toByteArray(Charset.forName("GBK")))
    }

    /**
     * 字节数组通过 GBK 编码成字符串
     *
     * @param byteArray 字节数组
     * @return 编码后的字符串
     */
    fun byteArray2GBKString(byteArray: ByteArray): String {
        return String(byteArray, Charset.forName("GBK"))
    }
}