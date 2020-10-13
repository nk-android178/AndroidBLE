package com.nk.androidble.ble;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

//import com.skyworth.ApartmentLock.main.entity.Password;
//import com.skyworth.ApartmentLock.main.room.NBOperaServer;
//import com.skyworth.ApartmentLock.main.room.OperaServer;

public class util {


    /**
     * 把16进制字符串转换成字节数组
     *
     * @return byte[]
     */

//    public static byte[] hexStringToByte(String hexString) {
//        int len = (hexString.length() / 2);
//        byte[] result = new byte[len];
//        char[] achar = hexString.toCharArray();
//        for (int i = 0; i < len; i++) {
//            int pos = i * 2;
//            result[i] = (byte) (charToByte(achar[pos]) << 4 | charToByte(achar[pos + 1]));
//        }
//        return result;
//    }
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }


    /*
     * 16进制字符串转字节数组
     */
    public static byte[] hexStringToByte(String hex) {

        if ((hex == null) || (hex.equals(""))) {
            return null;
        } else if (hex.length() % 2 != 0) {
            return null;
        } else {
            hex = hex.toUpperCase();
            int len = hex.length() / 2;
            byte[] b = new byte[len];
            char[] hc = hex.toCharArray();
            for (int i = 0; i < len; i++) {
                int p = 2 * i;
                b[i] = (byte) (charToByte(hc[p]) << 4 | charToByte(hc[p + 1]));
            }
            return b;
        }

    }

    /**
     * 把转换成字节数组16进制字符串
     *
     * @param bytes
     * @return byte[]
     */
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) { // 使用String的format方法进行转换
            buf.append(String.format("%02x", new Integer(b & 0xff)));
        }
        return buf.toString();
    }

    /**
     * 异或校验
     *
     * @param data 十六进制串
     * @return checkData  十六进制串
     */
    public static String checkXor(String data) {
        Log.e("util", "data =" + data);
        int checkData = 0;
        for (int i = 0; i < data.length(); i = i + 2) {
            //将十六进制字符串转成十进制
            int start = Integer.parseInt(data.substring(i, i + 2), 16);
            //进行异或运算
            checkData = start ^ checkData;
        }
        Log.e("util", "integerToHexString(checkData) =" + integerToHexString(checkData));
        return integerToHexString(checkData);
    }

    /**
     * 将十进制整数转为十六进制数，并补位
     */
    public static String integerToHexString(int s) {
        String ss = Integer.toHexString(s);
        if (ss.length() % 2 != 0) {
            ss = "0" + ss;//0F格式
        }
        return ss.toUpperCase();
    }

    /**
     * 将10进制自增后转成16进制
     */
    public static String atob(int a) {
        Log.e("Util", "integerToHexString 1= " + integerToHexString(a));
        String qq = integerToHexString(a);
        if (qq.length() < 8) {
            if (qq.length() == 2) {
                qq = "000000" + qq;
            }
            if (qq.length() == 3) {
                qq = "00000" + qq;
            }
            if (qq.length() == 4) {
                qq = "0000" + qq;
            }
            if (qq.length() == 5) {
                qq = "000" + qq;
            }
            if (qq.length() == 6) {
                qq = "00" + qq;
            }
            if (qq.length() == 7) {
                qq = "0" + qq;
            }
        }
        Log.e("Util", "integerToHexString 2= " + qq);
        return qq;
    }

    /**
     * 将十进制整数转为十六进制数，并补位 位数比较大
     */
    public static String ToHexStrings(String i) {
        long b = Long.parseLong(i);
        String ss = Long.toHexString(b);
        if (ss.length() % 2 != 0) {
            ss = "0" + ss;//0F格式
        }
        return ss.toUpperCase();
    }

    /**
     * 取反
     */
    public static String parseHex2Opposite(String str) {
        String hex;
        //十六进制转成二进制
        byte[] er = parseHexStr2Byte(str);

        //取反
        byte erBefore[] = new byte[er.length];
        for (int i = 0; i < er.length; i++) {
            erBefore[i] = (byte) ~er[i];
        }

        //二进制转成十六进制
        hex = parseByte2HexStr(erBefore);

        // 如果不够校验位的长度，补0,这里用的是两位校验
        hex = (hex.length() < 2 ? "0" + hex : hex);

        return hex;
    }

    /**
     * 将二进制转换成十六进制
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * 将十六进制转换为二进制
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    public static String checktime(String time) {
        String newTime = "";
        if (time.length() == 0) {
            newTime = "00";
        } else if (time.length() == 1) {
            newTime = "0" + time;
        } else {
            newTime = time;
        }
        return newTime;
    }


    // utc 时间转化

    private static final int DAY = 24 * 60 * 60;
    //开始年份
    private static final int EPOCH = 2000;
    //默认时间格式化字符串
    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 判断是否为闰年
     *
     * @param year
     * @return
     */
    public static boolean isLeapYear(int year) {
        return ((year % 400) == 0 || (((year % 100) != 0) && ((year % 4) == 0))) ? true : false;
    }

    /**
     * 根据月份获取当月的天数
     *
     * @param isLeapYear
     * @param month
     * @return
     */
    public static int getDaysOfMonth(boolean isLeapYear, int month) {
        int days = 31;
        if (month == 1) {//二月
            days = isLeapYear ? 29 : 28;
        } else {
            if (month > 6) {
                month--;
            }
            if ((month & 1) == 1) {
                days = 30;
            }
        }
        return days;
    }

    /**
     * 获取一年的天数
     *
     * @param year
     * @return
     */
    public static int getDaysOfYear(int year) {
        return isLeapYear(year) ? 366 : 365;
    }


    public static Date convertFromUTCSeconds(long time) {
        long seconds = time % DAY;
        int second = (int) seconds % 60;
        int minute = (int) (seconds % 3600) / 60;
        int hour = (int) seconds / 3600;

        long days = time / DAY;
        int year = EPOCH;
        while (days >= getDaysOfYear(year)) {
            days -= getDaysOfYear(year);
            year++;
        }

        int month = 0;
        while (days >= getDaysOfMonth(isLeapYear(year), month)) {
            days -= getDaysOfMonth(isLeapYear(year), month);
            month++;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, (int) days + 1, hour, minute, second);

        return calendar.getTime();
    }

    public static long convertToUTCSeconds(String time) throws ParseException {
        return convertToUTCSeconds(time, PATTERN);
    }

    /**
     * 根据本地时间转换为UTC时间的秒数
     *
     * @param time
     * @param pattern
     * @return
     * @throws ParseException
     */
    public static long convertToUTCSeconds(String time, String pattern) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        Date date = sdf.parse(time);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int second = calendar.get(Calendar.SECOND);
        int minute = calendar.get(Calendar.MINUTE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        long seconds = hour * 60 * 60 + minute * 60 + second;

        //获取当月完整的天数
        long days = day - 1;

        //获取完整月份的天数
        while (--month >= 0) {
            days += getDaysOfMonth(isLeapYear(year), month);
        }

        //获取完整年份的天数

        while (--year >= EPOCH) {
            days += getDaysOfYear(year);
        }
        return seconds + days * DAY;
    }


    /**
     * 当地时间 ---> UTC时间
     *
     * @return
     */
    public static String Local2UTC() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("gmt"));
        String gmtTime = sdf.format(new Date());
        return gmtTime;

    }

    /**
     * 判断服务是否启动
     */
//    public static boolean ServerIsRun(Context context, String servername) {
//        if (BaseActivity.lockChannel == 2) {
//            if (!OperaServer.isrunbleserver) {
//                return false;
//            }
//        } else if (BaseActivity.lockChannel == 3) {
//            if (!NBOperaServer.isrunbleserver) {
//                return false;
//            }
//        }
//
//        ActivityManager myManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager.getRunningServices(30);
//        for (int i = 0; i < runningService.size(); i++) {
//            if (runningService.get(i).service.getClassName().toString().equals(servername)) //"come.xuexin.test"
//            {
//                return true;
//            }
//        }
//        return false;
//    }

    /*
    数字每位前面加0   限制6位数字
     */
    public static String adminpass(String admin) {
        StringBuffer qq = new StringBuffer();
        for (int i = 0; i < 6; i++) {
            String ee = admin.substring(i, i + 1);
            qq.append("0" + ee);
        }
        return String.valueOf(qq);
    }

    /*
倒序取反  utc
 */
    public static String qufang(String utc) {
        String a = null;
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            a = utc.substring(utc.length() - ((i + 1) * 2), utc.length() - (i * 2));
            b = b.append(a);
        }
        return String.valueOf(b);
    }

    public static String qufang2(String utc) {
        String a = null;
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < 2; i++) {
            a = utc.substring(utc.length() - ((i + 1) * 2), utc.length() - (i * 2));
            b = b.append(a);
        }
        return String.valueOf(b);
    }

    /**
     * 处理userid
     */
    public static String userid(String userid) {
        int value = Integer.parseInt(userid);
        String strHex = Integer.toHexString(value);
        if (strHex.length() == 3) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(strHex.substring(1));
            stringBuffer.append("0");
            stringBuffer.append(strHex.substring(0, 1));
            Log.e("util", "userid 16 3=" + String.valueOf(stringBuffer));
            return String.valueOf(stringBuffer);
        }
        if (strHex.length() == 4) {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(strHex.substring(2));
            stringBuffer.append(strHex.substring(0, 2));
            Log.e("util", "userid 16 4=" + String.valueOf(stringBuffer));
            return String.valueOf(stringBuffer);
        }
        return "0000";
    }

    //nb 10转16进制
    public static String NBuserid(String userid) {
        return ToHexStrings(userid);

    }

    //密码异或加密 密码数据与 key[6] = {0x46, 0x45, 0x49, 0x42, 0x49, 0x47}进行异或运算
    public static String pwdxor(String pwd) {
//        Log.e("加密后密码111", "pwd =" + pwd);
        String pwdget = "";
        byte[] pwd16 = hexStringToByte(adminpass(pwd));
        byte[] pwds = {0x46, 0x45, 0x49, 0x42, 0x49, 0x47};
        byte[] mypwd = new byte[6];
//        Log.e("加密后密码", "pwd16 =" + String.valueOf(pwd16));
        StringBuffer stringBuffer = new StringBuffer();
        for (int a = 0; a < 6; a++) {
            mypwd[a] = (byte) (pwd16[a] ^ pwds[a]);
            stringBuffer = stringBuffer.append(ToHexStrings(String.valueOf(mypwd[a])));
//            Log.e("加密后密码", a+"pwd16[a] ="+String.valueOf(pwd16[a]));
//            Log.e("加密后密码", a+"pwds[a] ="+String.valueOf(pwds[a]));
//            Log.e("加密后密码", a+"mypwd[a] ="+String.valueOf(mypwd[a]));
//            Log.e("加密后密码", a+"stringBuffer ="+stringBuffer);
        }
//        pwdget = String.valueOf(Integer.valueOf(bytesToHexString(mypwd),16));;
        pwdget = String.valueOf(stringBuffer);
//        Log.e("----加密后密码----", pwdget);
        System.out.println("----加密后密码----" + pwdget);
        return pwdget;
    }

    public static byte[] putUserIds(List<Integer> userIds) {
        byte[] data = new byte[4];
        for (Integer userId : userIds) {
            int index = (userId - 1) / 8;
            int pos = (userId % 8 == 0) ? 0 : 8 - userId % 8;
            data[index] |= 0x1 << pos;
        }
        return data;
    }


    //有效的用户数据
    public static List<Integer> getActiveUserIds(byte[] users, byte[] states) {
        List<Integer> userIds = new ArrayList<>();
        int userId = 1;
        for (int i = 0; i < users.length; i++) {
            byte user = users[i];
            byte state = states[i];
            for (int pos = 7; pos >= 0; pos--) {
                if ((byte) ((user >> pos) & 0x1) == (byte) 1) {
                    if ((byte) ((state >> pos) & 0x1) == (byte) 1) {
                        userIds.add(userId);
                    }
                }
                userId++;
            }
        }
        return userIds;
    }

    // 无效的用户数据
    public static List<Integer> getInactiveUserIds(byte[] users, byte[] states) {
        List<Integer> userIds = new ArrayList<>();
        int userId = 1;
        for (int i = 0; i < users.length; i++) {
            byte user = users[i];
            byte state = states[i];
            for (int pos = 7; pos >= 0; pos--) {
                if ((byte) ((user >> pos) & 0x1) == (byte) 1) {
                    if ((byte) ((state >> pos) & 0x1) == (byte) 0) {
                        userIds.add(userId);
                    }
                }
                userId++;
            }
        }
        return userIds;
    }

//    public static String zhouqimima(Password cmd , int type){
////        byte[] pwd = Base64.decodeBase64(cmd.getPassword().getBytes());
//        byte[] pwd = hexStringToByte(adminpass(cmd.getPassword()));
//        byte[] data = new byte[22];
//        byte[] password = new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff};
//        for (int i = 0; i < pwd.length; i++) {
//            password[i] = pwd[i];
//        }
//        // 1 add   2 update
//        if(type == 1){
//            System.arraycopy(new byte[]{0x01}, 0, data, 0, 1);
//        }else if(type == 2){
//            System.arraycopy(new byte[]{0x04}, 0, data, 0, 1);
//        }
////        System.arraycopy(putUInt8((short) cmd.getAction()), 0, data, 0, 1);
//        System.arraycopy(new byte[]{0x02}, 0, data, 1, 1);
//        System.arraycopy(putUInt16(cmd.getDeviceUserId()), 0, data, 2, 2);
//        System.arraycopy(putUInt16(cmd.getCategory()), 0, data, 4, 2);
//        System.arraycopy(encrypt(password), 0, data, 6, 8);
//        System.arraycopy(putUInt8((short) cmd.getDaysMask()), 0, data, 14, 1);
//        System.arraycopy(putUInt8((short) cmd.getStartHour()), 0, data, 15, 1);
//        System.arraycopy(putUInt8((short) cmd.getStartMinute()), 0, data, 16, 1);
//        System.arraycopy(putUInt8((short) cmd.getEndHour()), 0, data, 17, 1);
//        System.arraycopy(putUInt8((short) cmd.getEndMinute()), 0, data, 18, 1);
//        System.arraycopy(new byte[]{0, 0, 0}, 0, data, 19, 3);
//        Log.e("添加周期数据","data = "+bytesToHexString(data));
//        return bytesToHexString(data);
//    }

    public static byte[] putUInt16(int n) {
        byte[] array = new byte[2];
        array[0] = (byte) (n & 0xff);
        array[1] = (byte) ((n >> 8) & 0xff);
        return array;
    }

    public static byte[] putUInt8(short n) {
        byte[] b = new byte[1];
        b[0] = (byte) (n & 0xff);
        return b;
    }

    public static byte[] encrypt(byte[] password) {
        byte key[] = {0x46, 0x45, 0x49, 0x42, 0x49, 0x47, 0x46, 0x45, 0x49, 0x42, 0x49, 0x47};// 加密密钥
        byte newPaString[] = new byte[password.length];// 密文
        for (int i = 0; i < password.length; i++) {
            newPaString[i] = (byte) (key[i] ^ password[i]);
        }
        return newPaString;
    }

    public static void main(String[] arg) {

        List<String> a = new ArrayList<>();
        a.add("1");
        a.add("2");
        a.add("3");
        a.add("4");
        a.add("5");
        a.add("6");

        a.remove(0);
        System.out.println(a.toString());
        System.out.println(a.get(0));
        a.remove(0);
        System.out.println(a.toString());
        System.out.println(a.get(0));
        a.remove(0);
        System.out.println(a.toString());
        System.out.println(a.get(0));
//        a.remove(0);
        a.clear();
        if (a.size() > 0) {
            System.out.println("asasas");
            System.out.println(a.get(0));
        }
        System.out.println(a.toString());


    }

}
