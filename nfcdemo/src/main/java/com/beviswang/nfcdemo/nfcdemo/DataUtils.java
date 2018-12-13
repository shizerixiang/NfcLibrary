package com.beviswang.nfcdemo.nfcdemo;

public class DataUtils {
    public static String bytesToHexString(byte[] src, boolean isPrefix) {
        StringBuilder stringBuilder = new StringBuilder();
        if (isPrefix == true) {
            stringBuilder.append("0x");
        }
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (byte aSrc : src) {
            buffer[0] = Character.toUpperCase(Character.forDigit((aSrc >>> 4) & 0x0F, 16));
            buffer[1] = Character.toUpperCase(Character.forDigit(aSrc & 0x0F, 16));
            //System.out.println(buffer);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    public static String saveHex2String(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (byte aData : data) {
            int value = aData & 0xff;
            sb.append(HEX[value / 16]).append(HEX[value % 16]).append(" ");
        }
        return sb.toString();
    }

    public static byte byteToHex(byte arg) {
        byte hex = 0;
        if (arg >= 48 && arg <= 57) {
            hex = (byte) (arg - 48);
        } else if (arg >= 65 && arg <= 70) {
            hex = (byte) (arg - 55);
        } else if (arg >= 97 && arg <= 102) {
            hex = (byte) (arg - 87);
        }
        return hex;
    }
    public static int byteToInt(byte[] b, int n) {
        int ret = 0;
        for (int i = 0; i < n; i++) {
            ret = ret<<8;
            ret |= b[i] & 0x00FF;
        }
        if (ret > 100000 || ret < -100000)
            ret -= 0x80000000;
        return ret;
    }
}
