package br.com.asfdev.chainway.services;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import android.util.Log;

import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.ConnectionStatus;
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback;
import com.rscja.deviceapi.interfaces.KeyEventCallback;

import java.util.List;

import br.com.asfdev.chainway.interfaces.BluetoothCallback;
import br.com.asfdev.chainway.interfaces.GripCallback;
import br.com.asfdev.chainway.interfaces.InventoryCallback;
import br.com.primeinterway.btrfid.BluetoothRFID;
import br.com.primeinterway.btrfid.interfaces.LocatorCallback;

public class RFIDService implements KeyEventCallback, BluetoothCallback {

    private final String TAG = getClass().getName();

    private Activity activity;
    private ConnectionStatusCallback connectionCallback;
    private GripCallback gripCallback;
    private LocatorCallback locatorCallback;

    private BluetoothService bluetoothService;

    private BluetoothRFID reader;
    private boolean isReading = false;

    String lastAddress;

    public RFIDService(Activity activity, ConnectionStatusCallback connectionCallback, GripCallback gripCallback, LocatorCallback callback) {
        this.activity = activity;
        this.connectionCallback = connectionCallback;
        this.gripCallback = gripCallback;
        this.locatorCallback = callback;

        this.bluetoothService = new BluetoothService(activity, this);

        reader = new BluetoothRFID();
        reader.init(activity);
    }

    public boolean init(String address) {
        Looper.prepare();

        lastAddress = address;
        searchDevices(address);

        reader.connect(address, (connectionStatus, o) -> {
        });
        reader.setConnectionStatusCallback(connectionCallback);

        long t = System.currentTimeMillis();
        long end = t + (5000);
        while (System.currentTimeMillis() < end) {
            if (reader.getConnectStatus() == ConnectionStatus.CONNECTED) {
                reader.setLocatorCallback(locatorCallback);
                return true;
            }
        }
        reader.free();
        return false;
    }

    public boolean dispose() {
        if (reader != null) {
            if (reader.getConnectStatus() == ConnectionStatus.CONNECTED) {
                return reader.free();
            }
        }
        return false;
    }

    public void registerKeyCallback() {
        reader.setKeyEventCallback(this);
    }

    public boolean isConnected() {
        if (reader != null) {
            return reader.getConnectStatus() == ConnectionStatus.CONNECTED;
        }
        return false;
    }

    public boolean isReading() {
        return this.isReading;
    }

    public void startInventory(InventoryCallback callback) {
        if (reader == null) return;

        isReading = true;
        reader.startInventoryTag();
        new Thread() {
            @Override
            public void run() {
                while (isReading) {
                    List<UHFTAGInfo> tags = reader.readTagFromBufferList_EpcTidUser();
                    if (tags != null) {
                        callback.receivedTags(tags);
                    }
                }
            }
        }.start();
    }

    public void stopInventory() {
        isReading = false;
        if (reader != null) {
            reader.stopInventory();
        }
    }

    // Leitura singular. Método simples, informando apenas EPC (irá filtrar Banco EPC, TAGs de 24 caracteres).
    public int singleLocation(String epc) {
        if (reader == null) return -1;
        return reader.getLocation(epc);
    }

    // Leitura singular. Método completo, deve informar banco, pontador, contador e EPC (irá filtrar de acordo com o preenchimento).
    public int singleLocation(int bank, int ptr, int cnt, String epc) {
        if (reader == null) return -1;
        return reader.getLocation(bank, ptr, cnt, epc);
    }

    // Leitura automática. Método simples, informando apenas EPC (irá filtrar Banco EPC, TAGs de 24 caracteres).
    public void startAutoLocation(String epc) {
        if (reader == null) return;
        reader.startAutoLocation(epc);
    }

    // Leitura automática. Método completo, deve informar banco, pontador, contador e EPC (irá filtrar de acordo com o preenchimento).
    public void startAutoLocation(int bank, int ptr, int cnt, String epc) {
        if (reader == null) return;
        reader.startAutoLocation(bank, ptr, cnt, epc);
    }

    public void stopAutoLocation() {
        if (reader == null) return;
        reader.stopAutoLocation();
    }

    public boolean isAutoLocation() {
        if (reader != null) {
            return reader.isAutoLocation();
        }
        return false;
    }


    @Override
    public void onKeyDown(int i) {
        if (reader == null) return;
        gripCallback.onClick();
    }

    @Override
    public void onKeyUp(int i) {

    }

    private void searchDevices(String address) {
//        reader.startScanBTDevices((bluetoothDevice, i, bytes) -> {
//            if(isConnected()) {
//                reader.stopScanBTDevices();
//            }
//            Log.d(TAG, "received: " + bluetoothDevice.getAddress());
//            if (bluetoothDevice.getAddress().equalsIgnoreCase(address)) {
//                reader.stopScanBTDevices();
//            }
//        });
        new Thread(() -> {
            try {
                bluetoothService.scanLeDevice(true);
                Thread.sleep(5000);
                bluetoothService.scanLeDevice(false);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }).start();
    }

    @Override
    public void findDevice(BluetoothDevice device) {
        if(device.getAddress().equalsIgnoreCase(lastAddress)) {
            Log.d(TAG, "findDevice: " + lastAddress);
            bluetoothService.scanLeDevice(false);
            bluetoothService.connect(lastAddress);
        }
    }
}
