package com.nk.androidble.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.UUID;

import static com.nk.androidble.ble.util.bytesToHexString;

public class BluetoothService extends Service {

    private final static String TAG = BluetoothService.class.getSimpleName();
    //蓝牙相关类
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;

    public final static String ACTION_GATT_CONNECTED = "com.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.bluetooth.le.EXTRA_DATA";
    public final static String CALL = "com.bluetooth.call";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    //关闭所有蓝牙连接
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    // 连接远程蓝牙
    public boolean connect(final String address, Context context) {
        if (mBluetoothAdapter == null || address == null) {
            Log.e(TAG, "连接远程蓝牙 BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.e(TAG, "连接远程蓝牙 Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect())//连接蓝牙，其实就是调用BluetoothGatt的连接方法
            {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        /* 获取远端的蓝牙设备 */
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.e(TAG, "连接远程蓝牙 Device not found.  Unable to connect.");
            return false;
        }

        /* 调用device中的connectGatt连接到远程设备 */
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.e(TAG, "连接远程蓝牙 Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
//        蓝牙开启：int STATE_ON，值为12，蓝牙模块处于开启状态；
//        蓝牙开启中：int STATE_TURNING_ON，值为11，蓝牙模块正在打开；
//        蓝牙关闭：int STATE_OFF，值为10，蓝牙模块处于关闭状态；
//        蓝牙关闭中：int STATE_TURNING_OFF，值为13，蓝牙模块正在关闭；
        Log.e(TAG, "连接远程蓝牙 device.getBondState==" + device.getBondState());
        System.out.println("连接远程蓝牙 device.getBondState==" + device.getBondState());
        if (device.getBondState() == 10) {
            Toast.makeText(context, "蓝牙模块处于关闭状态", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    //连接远程设备回调
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.e(TAG, "--onPhyUpdate--");
//            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            Log.e(TAG, "--onPhyRead--");
//            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        //连接状态改变
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG, "--onConnectionStateChange--");
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED)//连接成功
            {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                /* 通过广播更新连接状态 */
                broadcastUpdate(intentAction);
                Log.e(TAG, "连接成功 Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:"
                        + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)//连接失败
            {
                close();
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.e(TAG, " 连接失败 Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
//            super.onConnectionStateChange(gatt, status, newState);
        }

        /*
         * 重写onServicesDiscovered，发现蓝牙服务
         *
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.e(TAG, "--onServicesDiscovered--");
            if (status == BluetoothGatt.GATT_SUCCESS)//发现到服务
            {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.i(TAG, "--onServicesDiscovered called--");
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
                System.out.println("onServicesDiscovered received: " + status);
            }
//            super.onServicesDiscovered(gatt, status);
        }
        /*
         * 特征值的读
         * */

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG, "--onCharacteristicRead--");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "--onCharacteristicRead called--");
                //从特征值读取数据
                byte[] sucString = characteristic.getValue();
                String string = new String(sucString);
                //将数据通过广播到Ble_Activity
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, "read");
            }
//           super.onCharacteristicRead(gatt, characteristic, status);
        }

        /*
         * 特征值的写
         * */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e(TAG, "--onCharacteristicWrite--");
            // 以下语句实现 发送完数据或也显示到界面上
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, "write");
            byte[] sucString = characteristic.getValue();

            String string = bytesToHexString(sucString);
            Log.e(TAG, "--onCharacteristicWrite--" + string);
//            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        /*
         * 特征值的改变
         * */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "--onCharacteristicChanged--");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic, "changed");
//            super.onCharacteristicChanged(gatt, characteristic);
        }

        /*
         * 读描述值
         * */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.e(TAG, "--onDescriptorRead--");
            Log.w(TAG, "----onDescriptorRead status: " + status);
            byte[] desc = descriptor.getValue();
            if (desc != null) {
                Log.w(TAG, "----onDescriptorRead value: " + new String(desc));
            }
//            super.onDescriptorRead(gatt, descriptor, status);
        }

        /*
         * 写描述值
         * */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.e(TAG, "--onDescriptorWrite--");
            byte[] desc = descriptor.getValue();
            if (desc != null) {
                Log.w(TAG, "----onDescriptorWrite value: " + new String(desc));
            }
//            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.e(TAG, "--onReliableWriteCompleted--");
//            super.onReliableWriteCompleted(gatt, status);
        }

        /*
         * 读写蓝牙信号值
         * */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.e(TAG, "--onReadRemoteRssi--");
            broadcastUpdate(ACTION_DATA_AVAILABLE, rssi);
//            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.e(TAG, "--onMtuChanged--");
//            super.onMtuChanged(gatt, mtu, status);
        }
    };

    //广播意图
    private void broadcastUpdate(final String action, int rssi) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, String.valueOf(rssi));
        sendBroadcast(intent);
    }

    //广播意图
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /* 广播远程发送过来的数据 */
    public void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final String call) {
        final Intent intent = new Intent(action);
        //从特征值获取数据 初步处理返回byte数组
        final byte[] data = characteristic.getValue();
//        if (data != null && data.length > 0)
//        {
//            final StringBuilder stringBuilder = new StringBuilder(data.length);
//            for (byte byteChar : data)
//            {
//                stringBuilder.append(String.format("%02X ", byteChar)); //字符串转十六进制
//
//                Log.e(TAG, "***broadcastUpdate: byteChar = " + byteChar);
//
//            }
//            intent.putExtra(EXTRA_DATA, new String(data));
        intent.putExtra(EXTRA_DATA, bytesToHexString(data));
        intent.putExtra(CALL, call);
        Log.e(TAG, "broadcastUpdate for  read data:" + bytesToHexString(data));

//        }
        sendBroadcast(intent);
    }

    /* service 中蓝牙初始化 */
    public boolean initialize() {
        if (mBluetoothManager == null) {   //获取系统的蓝牙管理器
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
     * @param @param characteristic（要读的特征值）
     * @return void    返回类型
     * @throws
     * @Title: readCharacteristic
     * @Description: TODO(读取特征值)
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    // 写入特征值
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);// 这里写入应该触发gatt 回调
    }

    // 读取RSSi
    public void readRssi() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readRemoteRssi();
    }

    /**
     * @param @param characteristic（特征值）
     * @param @param enabled （使能）
     * @return void
     * @throws
     * @Title: setCharacteristicNotification
     * @Description: TODO(设置特征值通变化通知)
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
//        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002901-0000-1000-8000-00805f9b34fb"));
//        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00002901-0000-1000-8000-00805f9b34fb"));
//        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(UUID.fromString("00001002-0000-1000-8000-00805f9b34fb"));
        if (enabled) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        boolean a = mBluetoothGatt.writeDescriptor(clientConfig);
        Log.e(TAG, "writesuccess =" + a);
    }

    /**
     * @param @param 无
     * @return void
     * @throws
     * @Title: getCharacteristicDescriptor
     * @Description: TODO(得到特征值下的描述值)
     */
    public void getCharacteristicDescriptor(BluetoothGattDescriptor descriptor) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readDescriptor(descriptor);
    }

    /**
     * @param @return 无
     * @return List<BluetoothGattService>
     * @throws
     * @Title: getSupportedGattServices
     * @Description: TODO(得到蓝牙的所有服务)
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;
        return mBluetoothGatt.getServices();
    }
}
