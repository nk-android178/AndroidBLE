package com.nk.androidble.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nk.androidble.R;

import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.pow;


public class DeviceListAdapter extends BaseAdapter {

    ArrayList<BluetoothDevice> bluetoothDevices;
    ArrayList<Integer> rssi;
    Context context;

    public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> bluetoothDevices, ArrayList<Integer> rssi) {
        this.context = context;
        this.bluetoothDevices = bluetoothDevices;
        this.rssi = rssi;
    }

    @Override
    public int getCount() {
        if (bluetoothDevices.size() > 0) {
            return bluetoothDevices.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if (bluetoothDevices.size() > 0) {
            return bluetoothDevices.get(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(context).inflate(R.layout.activity_item, viewGroup, false);
            holder.name = view.findViewById(R.id.name);
            holder.mac = view.findViewById(R.id.mac);
            holder.rssi = view.findViewById(R.id.rssi);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        if (bluetoothDevices.get(i).getName() != null) {
            holder.name.setText(bluetoothDevices.get(i).getName() + "");
            Log.e("DeviceListAdapter", "getName" + bluetoothDevices.get(i).getName());
        } else {
            holder.name.setText("null");
        }
        if (bluetoothDevices.get(i).getAddress() != null) {
            holder.mac.setText(bluetoothDevices.get(i).getAddress() + "");
        } else {
            holder.mac.setText("null");
        }
        if (rssi.get(i) != null) {
//            d = 10^((abs(RSSI) - A) / (10 * n))
//            d - 计算所得距离
//            RSSI - 接收信号强度（负值）
//            A - 发射端和接收端相隔1米时的信号强度
//            n - 环境衰减因子

            holder.rssi.setText("" + rssi.get(i) + "/" + rsssi(rssi.get(i)) + "m");

        } else {
            holder.rssi.setText("null");
        }

        return view;
    }

    public class ViewHolder {
        TextView name, mac, rssi;
    }

    public float rsssi(int rssi) {
        int iRssi = abs(rssi);
        float power = (float) ((iRssi - 59) / (10 * 2.0));
        return (float) pow(10, power);
    }
}
