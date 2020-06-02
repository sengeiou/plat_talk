package android_serialport_api;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import callback.SerialPortCallBackUtils;
import util.ByteUtil;
import util.HexUtil;
import util.MyUtil;

/**
 * Created by Administrator on 2018/5/31.
 */

public class SerialPortUtil {

    public static String TAG = "SerialPortUtil";

    /**
     * 标记当前串口状态(true:打开,false:关闭)
     **/
    public static boolean isFlagSerial = false;

    public static SerialPort serialPort = null;
    public static InputStream inputStream = null;
    public static OutputStream outputStream = null;
    public static Thread receiveThread = null;
    public static String strData = "";

    /**
     * 打开串口
     */
    public static boolean open(String pathname, int baudrate, int flags) {
        Log.e("aaa",pathname+"    123");
        boolean isopen = false;
        if (isFlagSerial) {
            return false;
        }
        try {
            serialPort = new SerialPort(new File(pathname), baudrate, flags);
            Log.e("7770",pathname+"    123");
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            receive();
            isopen = true;
            isFlagSerial = true;
        } catch (IOException e) {
            e.printStackTrace();
            isopen = false;
        }
        return isopen;
    }

    /**
     * 关闭串口
     */
    public static boolean close() {
        if (isFlagSerial) {
            return false;
        }
        boolean isClose = false;
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            isClose = true;
            isFlagSerial = false;//关闭串口时，连接状态标记为false
        } catch (IOException e) {
            e.printStackTrace();
            isClose = false;
        }
        return isClose;
    }

    /**
     * 发送串口指令
     */
    public static void sendString(String data) {
        if (!isFlagSerial) {
            return;
        }
        try {
            //字符串转化为16进制字符串
            String str16 = MyUtil.str2HexStr("你好");
            //将16进制字符串转化为字节数组
            byte[] mByte = HexUtil.decodeHex(str16);
            //outputStream.write(ByteUtil.hex2byte(data));
            outputStream.write(mByte);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收串口数据的方法
     */
    public static void receive() {
        if (receiveThread != null && !isFlagSerial) {
            return;
        }
        receiveThread = new Thread() {
            @Override
            public void run() {
                while (isFlagSerial) {
                    try {
                        byte[] readData = new byte[32];
                        if (inputStream == null) {
                            return;
                        }
                        int size = inputStream.read(readData);
                        if (size > 0 && isFlagSerial) {
                            //Log.e("TAG","接收");
                            strData = ByteUtil.byteToStr(readData, size);
                            Log.e("TAG","十六进制转字符串："+MyUtil.hexStr2Str(strData));
                            SerialPortCallBackUtils.doCallBackMethod(MyUtil.hexStr2Str(strData));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        receiveThread.start();
    }
}