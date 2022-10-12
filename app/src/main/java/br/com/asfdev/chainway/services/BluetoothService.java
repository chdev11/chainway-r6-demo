package br.com.asfdev.chainway.services;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.com.asfdev.chainway.interfaces.BluetoothCallback;
import br.com.asfdev.chainway.utils.Math;

public class BluetoothService {

    private final String TAG = getClass().getName();

    private Activity activity;
    private BluetoothCallback callback;

    private BluetoothAdapter adapter;
    private BluetoothManager manager;

    // Scan attributes
    private boolean scanning;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 10000;

    private List<BluetoothDevice> bluetoothDevices = new ArrayList<>();

    // Gatt attributes
    UUID HEART_RATE_SERVICE_UUID = Math.convertFromInteger(0x180D);
    UUID HEART_RATE_MEASUREMENT_CHAR_UUID = Math.convertFromInteger(0x2A37);
    UUID HEART_RATE_CONTROL_POINT_CHAR_UUID = Math.convertFromInteger(0x2A39);

    BluetoothGatt gatt;

    // Class
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public BluetoothService(Activity activity, BluetoothCallback callback) {
        this.activity = activity;
        this.callback = callback;

        this.manager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (adapter == null || !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, 1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (scanning) {
                        scanning = false;
                        adapter.stopLeScan(leScanCallback);
                        bluetoothDevices.clear();
                    }
                }
            }, SCAN_PERIOD);

            scanning = true;
            adapter.startLeScan(leScanCallback);
        } else {
            scanning = false;
            adapter.stopLeScan(leScanCallback);
            bluetoothDevices.clear();
        }
    }

    public void connect(String address) {
        for(BluetoothDevice device : bluetoothDevices) {
            if(device.getAddress().equalsIgnoreCase(address)) {
                Log.d(TAG, "connect: " + device.getAddress());
                gatt = device.connectGatt(activity, true, gattCallback);
            }
        }
    }

    // Callbacks
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
                                if (bluetoothDevice.getAddress().equalsIgnoreCase(device.getAddress())) {
                                    return;
                                }
                            }

                            bluetoothDevices.add(device);
                            callback.findDevice(device);
                            Log.d(TAG, "onLeScan: " + device.getAddress() + "/" + device.getName());
                        }
                    });
                }
            };

    BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "onServicesDiscovered: success");
                    } else {
                        Log.d(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    Log.d(TAG, "onConnectionStateChange: " + status + "/" + newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices();
                    }
                }
            };
}
