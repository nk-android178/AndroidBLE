package com.nk.androidble;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.nk.androidble.ble.BluetoothService;
import com.nk.androidble.ble.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.nk.androidble.ble.util.atob;
import static com.nk.androidble.ble.util.checkXor;
import static com.nk.androidble.ble.util.checktime;
import static com.nk.androidble.ble.util.convertToUTCSeconds;
import static com.nk.androidble.ble.util.hexStringToByte;
import static com.nk.androidble.ble.util.integerToHexString;

/*
操作类，如果需要对接多个BLE设备可以使用服务来处理本操作，对应多个操作服务进行切换
 */
public class BleActivtity extends Activity implements View.OnClickListener {

    private final static String TAG = BleActivtity.class.getSimpleName();

    //服务uuid
//    public static String UUID_SERVICE ="69400001-b5a3-f393-e0a9-e50e24dcca99";
    public static String UUID_SERVICE = "f000ffd0-0451-4000-b000-000000000000";
    //读特性uuid
//    public static String UUID_READ = "69400003-b5a3-f393-e0a9-e50e24dcca99";
    public static String UUID_READ = "00001002-0000-1000-8000-00805f9b34fb";
    //    public static String UUID_READ = "69401003-B5A3-F393-E0A9-E50E24DCCA99";
    //写特性uuid
//    public static String UUID_WRITE = "69400002-b5a3-f393-e0a9-e50e24dcca99";
    public static String UUID_WRITE = "00001001-0000-1000-8000-00805f9b34fb";
    //    public static String UUID_WRITE = "69401002-B5A3-F393-E0A9-E50E24DCCA99";
    String num1 = "";
    String num7 = "";
    String num8 = "";

    //蓝牙特性值 这里针对写特性
    private static BluetoothGattCharacteristic target_chara = null;

    private static BluetoothGattCharacteristic read_chara = null;

    public static String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static String EXTRAS_DEVICE_RSSI = "RSSI";

    private String mDeviceName, mDeviceAddress, mRssi;
    private TextView address, name, rssi;
    private TextView showcontent;
    private EditText input;
    private Button send, open, syntime, managetimecode, synctime;
    private TextView connect_state;
    private TextView gettext;

    int up = 0; // 自增id起始10进制数

    //蓝牙连接状态
    private boolean mConnected = false;
    private String status = "disconnected";

    //蓝牙service,负责后台的蓝牙服务
    private static BluetoothService mBluetoothLeService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        // TODO 从意图获取显示的蓝牙信息
        mDeviceName = getIntent().getExtras().getString(EXTRAS_DEVICE_NAME, "");
        mDeviceAddress = getIntent().getExtras().getString(EXTRAS_DEVICE_ADDRESS, "null");
        mRssi = getIntent().getExtras().getString(EXTRAS_DEVICE_RSSI, "");
        initview();
        initdata();
        /* TODO 启动蓝牙service */
        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver, makeFilter());
    }

    public void initview() {
        address = findViewById(R.id.address);
        name = findViewById(R.id.name);
        rssi = findViewById(R.id.rssi);
        connect_state = findViewById(R.id.connect_state);
        showcontent = findViewById(R.id.showcontent);
        input = findViewById(R.id.input);
        send = findViewById(R.id.send);
        send.setOnClickListener(this);
        open = findViewById(R.id.open);
        open.setOnClickListener(this);
        syntime = findViewById(R.id.syntime);
        syntime.setOnClickListener(this);
        managetimecode = findViewById(R.id.managetimecode);
        managetimecode.setOnClickListener(this);
        synctime = findViewById(R.id.synctime);
        synctime.setOnClickListener(this);
        gettext = findViewById(R.id.gettext);
    }

    public void initdata() {
        address.setText("" + mDeviceAddress);
        name.setText("" + mDeviceName);
        rssi.setText("" + mRssi);
    }

    //蓝牙服务绑定回调
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBluetoothLeService = ((BluetoothService.LocalBinder) iBinder).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.e(TAG, " initialize Bluetooth");
            // 根据蓝牙地址，连接设备
            mBluetoothLeService.connect(mDeviceAddress, BleActivtity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, " Disconnected initialize Bluetooth");
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //绑定广播接收器
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            //建立链接 通过蓝牙mac
            final boolean result = mBluetoothLeService.connect(mDeviceAddress, BleActivtity.this);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");

        num1 = "AA" + "0A" + "11" + "00000001" + "00" + "00000000000000000000"; // IEEE地址低位在前 后面补齐2位即 0000
        num7 = checkXor(num1);
        num8 = "55";
        String num = num1 + num7 + num8;
        Log.e(TAG, "num = " + num);
        if (target_chara != null && mBluetoothLeService != null) {
            target_chara.setValue(hexStringToByte(num));
            mBluetoothLeService.writeCharacteristic(target_chara);
            Log.e(TAG, "断开连接已经发送消息");
        }
        handler.removeCallbacks(runnable);

        //解除广播接收器
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
//        mBluetoothLeService.unbindService(mServiceConnection);
        mBluetoothLeService = null;

//        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalReceiver);
        super.onDestroy();
    }

    //接受服务发送过来的广播
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action))//Gatt连接成功
            {
                mConnected = true;
                status = getString(R.string.connection_device);
                //更新连接状态
                updateConnectionState(status);

                if (target_chara != null && mBluetoothLeService != null) {
                    Toast.makeText(BleActivtity.this, "可以配对", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BleActivtity.this, "连接失败，请重新连接！", Toast.LENGTH_SHORT).show();
                }

            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action))//Gatt连接失败
            {
                //连接断开后停止发送连接请求
                handler.removeCallbacks(runnable);
                mConnected = false;
                status = "已断开设备";
                AlertDialog.Builder builder = new AlertDialog.Builder(BleActivtity.this);
                builder.setTitle("提示：");
                builder.setMessage("连接已断开，请重新连接！");
                builder.setIcon(R.mipmap.disconnected);
                builder.setPositiveButton("重新连接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        Intent intent = new Intent(BleActivtity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
                builder.setNegativeButton("取消",
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
                //更新连接状态
                updateConnectionState(status);
                Log.d(TAG, "BroadcastReceiver :"
                        + "device disconnected");
            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))//发现GATT服务器
            {
                //获取设备的所有蓝牙服务
                displayGattServices(mBluetoothLeService
                        .getSupportedGattServices());
                Log.e(TAG, "BroadcastReceiver : device SERVICES_DISCOVERED");
            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action))//有效数据
            {
                //处理发送过来的数据
                displayData(Objects.requireNonNull(intent.getExtras()).getString(
                        BluetoothService.EXTRA_DATA), Objects.requireNonNull(intent.getExtras()).getString(
                        BluetoothService.CALL));
                Log.d(TAG, "BroadcastReceiver onData:"
                        + intent.getStringExtra(BluetoothService.EXTRA_DATA));
                gettext.setText("当前接收：" + intent.getStringExtra(BluetoothService.EXTRA_DATA));
            }
        }
    };


    /**
     * @param
     * @return void
     * @throws
     * @Title: displayGattServices
     * @Description: TODO(处理蓝牙服务)
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null)
            return;
        String uuid = null;
        String unknownServiceString = "unknown_service";
        String unknownCharaString = "unknown_characteristic";

        // 服务数据,可扩展下拉列表的第一级数据
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        // 特征数据（隶属于某一级服务下面的特征值集合）
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();

        // 部分层次，所有特征值集合
        ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gattService : gattServices) {

            // 获取服务列表
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            // 查表，根据该uuid获取对应的服务名称。SampleGattAttributes这个表需要自定义。

            gattServiceData.add(currentServiceData);
            Log.e(TAG, "循环所有服务 uuid =" + uuid);
            Log.d(TAG, "Service uuid:" + uuid);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();

            // 从当前循环所指向的服务中读取特征值列表
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // 对于当前循环所指向的服务中的每一个特性值
            for (final BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                Log.e(TAG, "当前服务所有特性值 uuid =" + uuid);
                if (gattCharacteristic.getUuid().toString().equals(UUID_READ)) {
                    // 测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
//                    mhandler.postDelayed(new Runnable()
//                    {
//                        @Override
//                        public void run()
//                        {
//                            mBluetoothLeService.readCharacteristic(gattCharacteristic);
//                        }
//                    }, 200);
                    // 接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                    //在里面填写的uuid 一般默认不变
                    // gattCharacteristic 对应的是read或者notify的uuid值 来设置开启通知
                    read_chara = gattCharacteristic;
                    testhandler.postDelayed(testrun, 100);
//                    mBluetoothLeService.setCharacteristicNotification(gattCharacteristic, true);
                }
                Log.e(TAG, "---初始化 gattCharacteristic.getUuid().toString()" + gattCharacteristic.getUuid().toString());
                Log.e(TAG, "---初始化 UUID_WRITE" + UUID_WRITE);
                if (gattCharacteristic.getUuid().toString().equals(UUID_WRITE)) {
                    target_chara = gattCharacteristic;
                    Log.e(TAG, "---初始化 target_chara");
                    // 设置数据内容
                    // 往蓝牙模块写入数据
                    mBluetoothLeService.writeCharacteristic(gattCharacteristic);
                }
                List<BluetoothGattDescriptor> descriptors = gattCharacteristic.getDescriptors();
                Log.e(TAG, "---循环所有特征描述descriptor size :" + descriptors.size());
                for (BluetoothGattDescriptor descriptor : descriptors) {
                    Log.e(TAG, "---循环所有特征描述descriptor UUID:" + descriptor.getUuid());
                    // 获取特征值的描述
                    mBluetoothLeService.getCharacteristicDescriptor(descriptor);
                    // mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,
                    // true);
                }

                gattCharacteristicGroupData.add(currentCharaData);
            }
            // 按先后顺序，分层次放入特征值集合中，只有特征值
            mGattCharacteristics.add(charas);
            // 构件第二级扩展列表（服务下面的特征值）
            gattCharacteristicData.add(gattCharacteristicGroupData);

        }

    }

    private Handler testhandler = new Handler();
    private Runnable testrun = new Runnable() {
        @Override
        public void run() {
            mBluetoothLeService.setCharacteristicNotification(read_chara, true);
        }
    };

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (issend) {  //分段发送时效密码
                sendnum();
            } else {
                if (isSync) { // 同步开锁记录
                    isSync = false;
                    syncunlockrecord1();
                    handler.postDelayed(runnable, 200);
                } else {
                    Log.e(TAG, "post");
                    up = up + 1;
                    String add = atob(up); // "00000001"
                    num1 = "AA" + "0A" + "20" + add + "00" + "0A000000000000000000"; // IEEE地址低位在前 后面补齐2位即 0000
                    num7 = checkXor(num1);
                    num8 = "55";
                    String num = num1 + num7 + num8;
                    Log.e(TAG, "num = " + num);
                    target_chara.setValue(hexStringToByte(num));
                    mBluetoothLeService.writeCharacteristic(target_chara);
                    Log.e(TAG, "保持连接已经发送消息");

                    handler.postDelayed(runnable, 5000);
                }
            }

        }
    };

    /**
     * @param @param rev_string(接受的数据)
     * @return void
     * @throws
     * @Title: displayData
     * @Description: TODO(接收到的数据在TextView上显示)
     */
    private void displayData(final String rev_string, final String call) {
//        rev_str += rev_string;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //解析获取的数据，可以展示出来
                Log.e(TAG, "接受到数据 = " + rev_string);
                Log.e(TAG, "接受到数据 = " + call);
                if (call.equals("changed")) {
                    String ww = rev_string.substring(rev_string.length() - 6, rev_string.length());
//                    String qq = ww.substring(0,2);
                    String tt = rev_string.substring(4, 6);
                    String qq = rev_string.substring(16, 18);
                    Log.e(TAG, "配对数据 3--" + qq);
                    if (tt.equals("10")) { //命令字10  配对校验
                        if (qq.equals("00")) {
                            handler.postDelayed(runnable, 100);
                            Log.e(TAG, "配对成功 qq=" + qq);
                            Toast.makeText(BleActivtity.this, "配对成功", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "配对失败--" + qq);
                            Toast.makeText(BleActivtity.this, "配对失败,errorcode = " + qq, Toast.LENGTH_LONG).show();
                        }
                    } else if (tt.equals("20")) { // 命令字20 连接成功
                        if (qq.equals("00")) {
                            Log.e(TAG, "心跳连接成功");
                        } else {
                            Log.e(TAG, "心跳连接失败--" + qq);
                            Toast.makeText(BleActivtity.this, "心跳连接失败,errorcode = " + qq, Toast.LENGTH_SHORT).show();
                        }
                    } else if (tt.equals("11")) { // 命令字11  断开连接成功
                        if (qq.equals("00")) {
                            Log.e(TAG, "断开连接成功");
                            Toast.makeText(BleActivtity.this, "断开连接成功", Toast.LENGTH_LONG).show();
                            up = 0;
                        } else {
                            Log.e(TAG, "断开连接失败--" + qq);
                            Toast.makeText(BleActivtity.this, "断开连接失败,errorcode = " + qq, Toast.LENGTH_LONG).show();
                        }
                    } else if (tt.equals("60")) {
                        if (qq.equals("00")) {
                            Log.e(TAG, "蓝牙开锁成功");
                            Toast.makeText(BleActivtity.this, "蓝牙开锁成功", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "蓝牙开锁失败--" + qq);
                            Toast.makeText(BleActivtity.this, "蓝牙开锁失败,errorcode = " + qq, Toast.LENGTH_LONG).show();
                        }
                    } else if (tt.equals("80")) {
                        if (qq.equals("00")) {
                            Log.e(TAG, "同步开锁记录完成");
                            Toast.makeText(BleActivtity.this, "同步开锁记录完成", Toast.LENGTH_LONG).show();
                        } else if (qq.equals("20")) {
                            Log.e(TAG, "同步开锁记录开始");
                            Toast.makeText(BleActivtity.this, "同步开锁记录开始", Toast.LENGTH_LONG).show();
                            syncunlockrecord();
                        } else {
                            Log.e(TAG, "同步开锁记录失败 --" + qq);
                            Toast.makeText(BleActivtity.this, "同步开锁记录失败,errorcode =" + qq, Toast.LENGTH_LONG).show();
                        }
                    } else if (tt.equals("30")) {
                        if (qq.equals("00")) {
                            Log.e(TAG, "同步时间成功");
                            Toast.makeText(BleActivtity.this, "同步时间成功", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "同步时间失败 --" + qq);
                            Toast.makeText(BleActivtity.this, "同步时间失败,errorcode =" + qq, Toast.LENGTH_LONG).show();
                        }
                    } else if (tt.equals("50")) {
                        if (qq.equals("00")) {
                            Log.e(TAG, "同步时间成功");
                            Toast.makeText(BleActivtity.this, "同步时间成功", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "同步时间失败 --" + qq);
                            Toast.makeText(BleActivtity.this, "同步时间失败,errorcode =" + qq, Toast.LENGTH_LONG).show();
                        }
                        Log.e(TAG, "管理管理员密码");
                        Toast.makeText(BleActivtity.this, "管理管理员密码", Toast.LENGTH_LONG).show();
                    } else if (tt.equals("51")) {
                        if (qq.equals("00")) {
                            Log.e(TAG, "管理时效密码成功");
                            Toast.makeText(BleActivtity.this, "管理时效密码成功", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "管理时效密码失败 --" + qq);
                            Toast.makeText(BleActivtity.this, "管理时效密码失败,errorcode =" + qq, Toast.LENGTH_LONG).show();
                        }
                    } else if (tt.equals("52")) {
                        if (qq.equals("00")) {
                            Log.e(TAG, "管理周期密码成功");
                            Toast.makeText(BleActivtity.this, "管理周期密码成功", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "管理周期密码失败 --" + qq);
                            Toast.makeText(BleActivtity.this, "管理周期密码失败,errorcode =" + qq, Toast.LENGTH_LONG).show();
                        }
                    } else if (tt.equals("5A") || tt.equals("5a")) {
                        if (qq.equals("00")) {
                            Log.e(TAG, "密码重置成功");
                            Toast.makeText(BleActivtity.this, "密码重置成功", Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "密码重置失败 --" + qq);
                            Toast.makeText(BleActivtity.this, "密码重置失败,errorcode =" + qq, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        });
    }


    /* 更新连接状态 */
    private void updateConnectionState(String status) {
        Message msg = new Message();
        msg.what = 1;
        Bundle b = new Bundle();
        b.putString("connect_state", status);
        msg.setData(b);
        //将连接状态更新的UI的textview上
        myHandler.sendMessage(msg);
        Log.d(TAG, "connect_state:" + status);
    }

    private Handler mhandler = new Handler();
    @SuppressLint("HandlerLeak")
    private Handler myHandler = new Handler() {
        // 2.重写消息处理函数
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1: {
                    String state = msg.getData().getString("connect_state");
                    Log.e(TAG, "connect_state =" + state);
                    connect_state.setText(state);
                    break;
                }

            }
            super.handleMessage(msg);
        }

    };

    /* 意图过滤器 */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {                 // 6066005694467784
            case R.id.send: // 发送校验消息 // 00158D00027558BA    00158D0001FF5F3C  00158D00029FCE96   158D00029FCEC8
//                num1 = "AA"+"0A"+"10"+"00000001"+"00"+"00158D00029FCEC80000"; // IEEE地址低位在前 后面补齐2位即 0000
//                num1 = "AA0A600000000100"+"03060909080700000000";
                num1 = "AA0A850000000100" + "E007071A0B1E2D000000";
                num7 = checkXor(num1);
                num8 = "55";
                String num = num1 + num7 + num8;
                Log.e(TAG, "num = " + num);
                sendData(hexStringToByte(num),target_chara);
                Log.e(TAG, "已经发送校验消息");
                break;
            case R.id.open: // 发送开锁消息 369987
                num1 = "AA" + "0A" + "60" + "00000001" + "00" + "03060909080700000000";
                num7 = checkXor(num1);
                num8 = "55";
                String num2 = num1 + num7 + num8;
                Log.e(TAG, "num = " + num2);
                sendData(hexStringToByte(num2),target_chara);
                Log.e(TAG, "已经发送开锁消息");
                break;
            case R.id.syntime: // 发送同步时间数据
                Calendar cal = Calendar.getInstance();
                int y = cal.get(Calendar.YEAR);
                int m = cal.get(Calendar.MONTH) + 1;
                int d = cal.get(Calendar.DATE);
                int h = cal.get(Calendar.HOUR_OF_DAY);
                int mi = cal.get(Calendar.MINUTE);
                int s = cal.get(Calendar.SECOND);
                String year = String.valueOf(y).substring(2, 4);

                Log.e(TAG, "现在时刻是" + year + "年" + m + "月" + d + "日" + h + "时" + mi + "分" + s + "秒");
                num1 = "AA" + "0A" + "30" + "00000001" + "00" + checktime(year) + checktime(String.valueOf(m)) + checktime(String.valueOf(d)) + checktime(String.valueOf(h)) + checktime(String.valueOf(mi)) + checktime(String.valueOf(s)) + "00000000"; // IEEE地址低位在前 后面补齐2位即 0000
                num7 = checkXor(num1);
                num8 = "55";
                String num3 = num1 + num7 + num8;
                Log.e(TAG, "num = " + num3);
                sendData(hexStringToByte(num3),target_chara);
                Log.e(TAG, "已经发送开锁消息");
                break;
            case R.id.managetimecode: // 管理时效密码
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// HH:mm:ss
                //获取当前时间
                Date date = new Date(System.currentTimeMillis()); // 13位时间戳 单位 毫秒  默认现在是开始时间
                Date date2 = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24);
                String startTime = null;
                String endTime = null;
                try {
                    //转化成utc后的16进制数
                    Log.e("MainActivity", "integerToHexString =" + integerToHexString((int) convertToUTCSeconds(simpleDateFormat.format(date))));
                    startTime = integerToHexString((int) convertToUTCSeconds(simpleDateFormat.format(date)));
                    endTime = integerToHexString((int) convertToUTCSeconds(simpleDateFormat.format(date2)));
                } catch (ParseException e) {
                    e.printStackTrace();  //234DFDB0     234F4F30  7FFE6C23
                }                                                        // UTC 时间 倒序
//                Log.e(TAG,"startTime = "+startTime);
//                Log.e(TAG,"util.qufang(startTime) = "+util.qufang(startTime));
//                Log.e(TAG,"simpleDateFormat.format(date) = "+simpleDateFormat.format(date));
                num1 = "AA" + "1C" + "51" + "00000001" + "00" + "3C00" + "1111" + util.qufang(startTime) + util.qufang(endTime) + "FF" + "01" + "06" + "010203040506" + "06" + "020202020405"; // IEEE地址低位在前 后面补齐2位即 0000
                num7 = checkXor(num1);
                num8 = "55";
                String num4 = num1 + num7 + num8;
                Log.e(TAG, "num = " + num4);
                String num5 = num4.substring(0, 40);
                String num11 = num4.substring(40, num4.length());
                num12 = num11;
                Log.e(TAG, "num5 =" + num5);
                Log.e(TAG, "num11 =" + num11);
                handler.removeCallbacks(runnable);
                sendData(hexStringToByte(num5),target_chara);
                issend = true;
                handler.postDelayed(runnable, 200);
                Log.e(TAG, "已经发送管理时效密码消息 1");
                break;
            case R.id.synctime:// 同步开门记录
                syncunlockrecord1();
                break;
        }
    }

    private void sendData(byte[] value, BluetoothGattCharacteristic characteristic) {
        target_chara.setValue(value);
        mBluetoothLeService.writeCharacteristic(characteristic);
    }

    private String num12;
    private boolean issend = false;

    public void sendnum() {
        target_chara.setValue(hexStringToByte(num12));
        mBluetoothLeService.writeCharacteristic(target_chara);
        issend = false;
        handler.postDelayed(runnable, 2000);
        Log.e(TAG, "已经发送管理时效密码消息 2");
    }

    private boolean isSync = false;

    //发起同步请求  1
    public void syncunlockrecord1() {
        num1 = "AA" + "01" + "80" + "00000001" + "00" + "FF"; // IEEE地址低位在前 后面补齐2位即 0000
        num7 = checkXor(num1);
        num8 = "55";
        String num6 = num1 + num7 + num8;
        Log.e(TAG, "num = " + num6);
        target_chara.setValue(hexStringToByte(num6));

        mBluetoothLeService.writeCharacteristic(target_chara);
        Log.e(TAG, "已经发送同步开锁记录消息");
    }

    //同步成功后回复给锁   2
    public void syncunlockrecord() {
        num1 = "AA" + "01" + "80" + "00000001" + "01" + "00"; // IEEE地址低位在前 后面补齐2位即 0000
        num7 = checkXor(num1);
        num8 = "55";
        String num6 = num1 + num7 + num8;
        Log.e(TAG, "num = " + num6);
        target_chara.setValue(hexStringToByte(num6));

        mBluetoothLeService.writeCharacteristic(target_chara);
        isSync = true;
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, 200);
        Log.e(TAG, "已经发送同步开锁记录消息2");
    }

    public void sendlock(String num) {
        synchronized (this) {
            target_chara.setValue(hexStringToByte(num));

            mBluetoothLeService.writeCharacteristic(target_chara);
        }

    }

//    private IntentFilter makeFilter() {
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_GATT_DISCONNECTED);
//        filter.addAction(ACTION_RSSI_AVAILABLE);
//        filter.addAction(ACTION_DATA_AVAILABLE);
//        return filter;
//    }
//
//    //各蓝牙事件的广播action
//    public static final String ACTION_CONNECT_TIMEOUT = ".LeProxy.ACTION_CONNECT_TIMEOUT";
//    public static final String ACTION_CONNECT_ERROR = ".LeProxy.ACTION_CONNECT_ERROR";
//    public static final String ACTION_GATT_CONNECTED = ".LeProxy.ACTION_GATT_CONNECTED";
//    public static final String ACTION_GATT_DISCONNECTED = ".LeProxy.ACTION_GATT_DISCONNECTED";
//    public static final String ACTION_GATT_SERVICES_DISCOVERED = ".LeProxy.ACTION_GATT_SERVICES_DISCOVERED";
//    public static final String ACTION_DATA_AVAILABLE = ".LeProxy.ACTION_DATA_AVAILABLE";
//    public static final String ACTION_RSSI_AVAILABLE = ".LeProxy.ACTION_RSSI_AVAILABLE";
//    public static final String ACTION_MTU_CHANGED = ".LeProxy.ACTION_MTU_CHANGED";
//
//    public static final String EXTRA_ADDRESS = ".LeProxy.EXTRA_ADDRESS";
//    public static final String EXTRA_DATA = ".LeProxy.EXTRA_DATA";
//    public static final String EXTRA_UUID = ".LeProxy.EXTRA_UUID";
//    public static final String EXTRA_RSSI = ".LeProxy.EXTRA_RSSI";
//    public static final String EXTRA_MTU = ".LeProxy.EXTRA_MTU";
//    public static final String EXTRA_STATUS = ".LeProxy.EXTRA_STATUS";
//
//    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//
//            String address = intent.getStringExtra(EXTRA_ADDRESS);
//
//            switch (intent.getAction()) {
//                case ACTION_GATT_DISCONNECTED:// 断线
////                    mSelectedAddresses.remove(address);
////                    mDeviceListAdapter.removeDevice(address);
//                    break;
//
//                case ACTION_RSSI_AVAILABLE: {// 更新rssi
////                    LeDevice device = mDeviceListAdapter.getDevice(address);
////                    if (device != null) {
////                        int rssi = intent.getIntExtra(LeProxy.EXTRA_RSSI, 0);
////                        device.setRssi(rssi);
////                        mDeviceListAdapter.notifyDataSetChanged();
////                    }
//                }
//                break;
//
//                case ACTION_DATA_AVAILABLE:// 接收到从机数据
//                    displayRxData(intent);
//                    break;
//            }
//        }
//    };
//
//    private void displayRxData(Intent intent) {
//        String address = intent.getStringExtra(EXTRA_ADDRESS);
//        String uuid = intent.getStringExtra(EXTRA_UUID);
//        byte[] data = intent.getByteArrayExtra(EXTRA_DATA);
//
////        LeDevice device = mDeviceListAdapter.getDevice(address);
////        if (device != null) {
//            String dataStr = "timestamp: "  + '\n'
//                    + "uuid: " + uuid + '\n'
//                    + "length: " + (data == null ? 0 : data.length) + '\n';
////            if (mBoxAscii.isChecked()) {
////                if (data == null) {
////                    dataStr += "data: ";
////                } else {
////                    dataStr += "data: " + new String(data);
////                }
////            } else {
//                dataStr += "data: " + DataUtil.byteArrayToHex(data) + '\n';
////            }
//            Log.e(TAG, "-----data----- ="+ dataStr);
////            device.setRxData(dataStr);
////            mDeviceListAdapter.notifyDataSetChanged();
////        }
//    }

}
