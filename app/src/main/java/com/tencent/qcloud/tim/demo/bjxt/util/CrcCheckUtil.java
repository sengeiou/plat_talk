package com.tencent.qcloud.tim.demo.bjxt.util;


public class CrcCheckUtil {
    /**
     * HexString2Bytes(String src)函数用来将16进制的字符串转为byte数组
     * @param bytes 字节数组
     * @return 十六进制表示的校验码
     */
    public static String CRC_XModem(byte[] bytes){
        int crc = 0x00;          // initial value
        int polynomial = 0x1021;
        for (int index = 0 ; index< bytes.length; index++) {
            byte b = bytes[index];
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }
        crc &= 0xffff;
        return intToHex(crc);
    }


    /**
     * 将两个ASCII字符合成一个字节； 如："EF"–> 0xEF
     * @param src0 byte
     * @param src1 byte
     * @return byte
     */
    private static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[] {src0})).byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 })).byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }


    /**
     * 将16进制字符串转为字节数组
     * @param src String
     * @return byte[]
     */
    public static byte[] HexString2Bytes(String src) {
        src = src.replaceAll("\\s*","");
        if (null == src || 0 == src.length()) {
            return null;
        }
        byte[] ret = new byte[src.length() / 2];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < (tmp.length / 2); i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }
    /**
     * 将十进制转16进制
     */
    private static String intToHex(int n) {
        StringBuffer s = new StringBuffer();
        char []b = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        while(n != 0){
            s = s.append(b[n%16]);
            n = n/16;
        }
        s.reverse();
        while (s.length() < 4) {		 //CRC检验一般为4位，不足4位补0
            s.insert(0,"0");
        }
        return s.toString();
    }
}
