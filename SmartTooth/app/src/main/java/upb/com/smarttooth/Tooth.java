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
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;

import adrian.upb.smarttooth.R;

public class Tooth {
    static public boolean online;
    private BluetoothAdapter bluetoothAdapter;
    int REQUEST_ENABLE_BT = 5;
    BluetoothGatt bluetoothGatt;
    public BluetoothGattCharacteristic phCharac;
    public BluetoothGattCharacteristic humCharac;
    public BluetoothGattCharacteristic TTCharac;
    public BluetoothGattCharacteristic TPCharac;
    public BluetoothGattCharacteristic TACharac;
    public BluetoothGattCharacteristic T1Charac;
    public BluetoothGattCharacteristic T2Charac;
    public BluetoothGattCharacteristic T3Charac;
    public BluetoothGattCharacteristic T4Charac;
    public BluetoothGattCharacteristic VCharac;
    public BluetoothGattCharacteristic STCharac;

    private static Tooth instance;
    public final DataFrame dataFrame;

    public Tooth(final Activity activity){
        instance = this;
        dataFrame = new DataFrame(new String[]{"PH", "Humidity"}, null, true);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(activity, "BLE not supported", Toast.LENGTH_SHORT).show();
            //TODO display more persistent error
            return;
        }

        Log.i("Smartooth", "Scanning started");
        bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
            BluetoothDevice foundDevice = null;

            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d("BluetoothScan", "device found - " + device.getAddress() + ", "
                        + device.getName() + ", " + device.getUuids());

                if(foundDevice != null) {
                    Log.d("BluetoothScan", "device already found");
                    return;
                }

                if(!Config.TOOTH_MACs.contains(device.getAddress())) {
                    Log.d("BluetoothScan", "device not in list - " + Config.TOOTH_MACs.toString());
                    return;
                }

                foundDevice = device;

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Got a device", Toast.LENGTH_LONG).show();
                    }
                });

                bluetoothAdapter.stopLeScan(this);

                bluetoothGatt = device.connectGatt(activity, false, btleGattCallback);
            }
        });

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

    public static Tooth getInstance() {
        return instance;
    }

    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("Characteristic was written ");
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            // this will get called anytime you perform a read or write characteristic operation
            final int value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            System.out.println("Value is " + value);
            if(characteristic == phCharac) {
                dataFrame.update(value, DataFrame.DataType.PH);
            }
            if(characteristic == humCharac) {
                dataFrame.update(value, DataFrame.DataType.Humidity);
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
            Tooth.online = newState == BluetoothProfile.STATE_CONNECTED;
            if(Tooth.online) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a            BluetoothGatt.discoverServices() call
            System.out.println("onServicesDiscovered status was " + status);
            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                if (service.getUuid().toString().contains(Config.TOOTH_UUID_IN_SERVICE)) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_IN_CHARAC_PH)) {
                            System.out.println("Found Ph");
                            phCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_IN_CHARAC_HUM)) {
                            System.out.println("Found Hum");
                            humCharac = characteristic;
                        }
                    }
                }
                if (service.getUuid().toString().contains(Config.TOOTH_UUID_OUT_SERVICE)) {
                    List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_TT)) {
                            System.out.println("Found TT");
                            TTCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_TA)) {
                            System.out.println("Found TA");
                            TACharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_TP)) {
                            System.out.println("Found TP");
                            TPCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_T1)) {
                            System.out.println("Found T1");
                            T1Charac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_T2)) {
                            System.out.println("Found T2");
                            T2Charac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_T3)) {
                            System.out.println("Found T3");
                            T3Charac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_T4)) {
                            System.out.println("Found T4");
                            T4Charac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_ST)) {
                            System.out.println("Found ST");
                            STCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_V)) {
                            System.out.println("Found V");
                            VCharac = characteristic;
                        }

                    }
                }
            }
        }
    };

    public void setCharact(EditText c, int v) {
        BluetoothGattCharacteristic charac = mapTextToCharac(c);
        if(charac != null ){
            charac.setValue(v, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            bluetoothGatt.writeCharacteristic(charac);
        }
    }

    private BluetoothGattCharacteristic mapTextToCharac(EditText c) {
        int id = c.getId();
        switch (id){
            case R.id.editText_Voltage: { return VCharac; }
            case R.id.numberPickerT1: { return T1Charac; }
            case R.id.numberPickerT2: { return T2Charac; }
            case R.id.numberPickerT3: { return T3Charac; }
            case R.id.numberPickerT4: { return T4Charac; }
            case R.id.numberPickerTA: { return TACharac; }
            case R.id.numberPickerTP: { return TPCharac; }
            case R.id.numberPickerTT: { return TTCharac; }
            default: return null;
        }
    }
}
