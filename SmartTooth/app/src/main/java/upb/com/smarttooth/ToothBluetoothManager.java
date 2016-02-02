package upb.com.smarttooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import org.achartengine.model.XYSeries;

import java.util.Date;
import java.util.List;

public class ToothBluetoothManager {
    private Activity caller;
    private BluetoothAdapter.LeScanCallback callback;
    private BluetoothAdapter bluetoothAdapter;
    int REQUEST_ENABLE_BT = 5;
    BluetoothDevice dev = null;
    BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic phCharac;
    BluetoothGattCharacteristic humCharac;
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            // this will get called anytime you perform a read or write characteristic operation
            final int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            System.out.println("Value is " + value);
            if(characteristic == phCharac) {
                Tooth.updatePH(value);
            }
            if(characteristic == humCharac) {
                Tooth.updateHUM(value);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            System.out.println("Value is " + characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            System.out.println("Characteristic " + characteristic.toString());
            for (final BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                System.out.println(descriptor.getValue());
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e("Smartooth", "Gatt found " + gatt);
            boolean online = newState == BluetoothProfile.STATE_CONNECTED;
            Tooth.getInstance().setOnline(online);
            if(online) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a            BluetoothGatt.discoverServices() call
            System.out.println("onServicesDiscovered status was " + status);
            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                if (service.getUuid().toString().contains(Config.TOOTH_UUID_SERVICE)) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_CHARAC_PH)) {
                            System.out.println("Found ");
                            phCharac = characteristic;
                            bluetoothGatt.readCharacteristic(phCharac);
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_CHARAC_HUM)) {
                            System.out.println("Found ");
                            humCharac = characteristic;
                        }
                    }
                }
            }
        }
    };
    public ToothBluetoothManager(){
        this.caller = MainActivity.instance;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            caller.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (!caller.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(caller, "BLE not supported", Toast.LENGTH_SHORT).show();
            MainActivity.error = "This app was designed for Bluetooth Low Energy which your OS does not support";
            return;
        }
        Log.i("Smartooth", "Scanning started");
        callback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.e("Smartooth adress" ,device.getAddress());
                Log.e("Smartooth", "Device found " + device.getName());
                Log.e("Smartooth", "With UUIDS " + device.getUuids());
                caller.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(device.getAddress().equals(Config.TOOTH_MAC)) {
                            dev = device;
                            Toast.makeText(caller, "Got a device", Toast.LENGTH_LONG).show();
                        } else {
                            Log.d("vertigo", (device.getAddress()));
                            Log.d("vertigo", Config.TOOTH_MAC);
                        }
                    }
                });

            }
        };
        bluetoothAdapter.startLeScan(callback);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(dev == null){
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                bluetoothAdapter.stopLeScan(callback);
                Log.d("Smarttooth", "Stopped scan");
                bluetoothGatt = dev.connectGatt(caller, false, btleGattCallback);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(Config.READ_INTERVAL);
                        if(bluetoothGatt != null) {
                            if (phCharac != null) {
                                bluetoothGatt.readCharacteristic(phCharac);
                            }
                            if (humCharac != null) {
                                bluetoothGatt.readCharacteristic(humCharac);
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

}
