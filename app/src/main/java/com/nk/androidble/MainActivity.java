package com.nk.androidble;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.nk.androidble.ble.BaseActivity;
import com.nk.androidble.ble.DeviceListAdapter;

import java.util.ArrayList;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private ListView devicelist;
    private Button refresh;
    private TextView tx_right;
    private Handler mHandler;

    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    DeviceListAdapter mDeviceListAdapter;

    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    ArrayList<Integer> mRssi = new ArrayList<>();

    // 蓝牙扫描时间
    private static final long SCAN_PERIOD = 10000;
    // 描述扫描蓝牙的状态
    private boolean mScanning; // 点击列表判断是否停止扫描
    private boolean scan_flag;// 扫描状态判断，避免重复调用

    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    int REQUEST_ENABLE_BT = 1;

    Animation animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBLE();
        initview();
        checkVersion();
    }

    public void initview() {
        animation = AnimationUtils.loadAnimation(this, R.anim.img_animation);
        LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
        animation.setInterpolator(lin);
        devicelist = findViewById(R.id.devicelist);
        devicelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final BluetoothDevice device = bluetoothDevices.get(i);
                if (device == null)
                    return;
                final Intent intent = new Intent(MainActivity.this, BleActivtity.class);
                intent.putExtra(BleActivtity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(BleActivtity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                intent.putExtra(BleActivtity.EXTRAS_DEVICE_RSSI, mRssi.get(i).toString());
                if (mScanning) {
                    /* 停止扫描设备 */
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        mDeviceListAdapter = new DeviceListAdapter(this, bluetoothDevices, mRssi);
        devicelist.setAdapter(mDeviceListAdapter);
        refresh = findViewById(R.id.start);
        refresh.setOnClickListener(this);
        tx_right = findViewById(R.id.tx_rights);
        mHandler = new Handler();
        scanLeDevice(true);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start: // 开始扫描
                if (scan_flag) {
                    bluetoothDevices.clear();
                    mRssi.clear();
                    scanLeDevice(true);
                } else {
                    scanLeDevice(false);
                }
                break;
        }
    }

    /**
     * @param enable (扫描使能，true:扫描开始,false:扫描停止)
     * @return void
     * @throws
     * @Title: scanLeDevice
     * @Description: TODO(扫描蓝牙设备)
     */
    private void scanLeDevice(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    scan_flag = true;
//                    scan_btn.setText("扫描设备");
                    Log.i("SCAN", "stop.....................");
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    tx_right.clearAnimation();
                }
            }, SCAN_PERIOD);
            /* 开始扫描蓝牙设备，带mLeScanCallback 回调函数 */
            Log.i("SCAN", "begin.....................");
            mScanning = true;
            scan_flag = false;
            //先扫描10秒，然后停止扫描
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            tx_right.startAnimation(animation);
        } else {
            Log.i("Stop", "stoping................");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            scan_flag = true;
            tx_right.clearAnimation();
        }

    }

    /**
     * 蓝牙扫描回调函数 实现扫描蓝牙设备，回调蓝牙BluetoothDevice，可以获取name MAC等信息
     **/
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            // TODO Auto-generated method stub

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e("---MainActvity----", "----run---");
                    //扫描到设备的信息输出到listview的适配器
                    //contains 比较后者是否在前一个集合中，有返回true 无返回false
                    if (!bluetoothDevices.contains(device)) {
                        bluetoothDevices.add(device);
                        mRssi.add(rssi);
                        mDeviceListAdapter.notifyDataSetChanged();
                    }
                }
            });
            Log.e("mainactivity", "Name:" + device.getName());
            Log.e("mainactivity", "Address:" + device.getAddress());
            Log.e("mainactivity", "rssi:" + rssi);
        }
    };

    /**
     * 初始化蓝牙
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void initBLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持BLE", Toast.LENGTH_LONG).show();
            return;
        }
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // 打开蓝牙权限，关闭会提示打开
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    //6.0以上需要定位权限
    public void checkVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//如果 API level 是大于等于 23(Android 6.0) 时
            //判断是否具有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要向用户解释为什么需要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, "自Android 6.0开始需要打开位置权限才可以搜索到Ble设备", Toast.LENGTH_LONG).show();
                }
                //请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ACCESS_COARSE_LOCATION);
            }
        }
    }
}
