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

import java.util.List;

public class Tooth {
    private Activity activity;
    static public boolean online;
    private BluetoothAdapter bluetoothAdapter;
    int REQUEST_ENABLE_BT = 5;
    BluetoothGatt bluetoothGatt;
    BluetoothGattCharacteristic phCharac;
    BluetoothGattCharacteristic humCharac;
    private static Tooth instance;
    public final DataFrame dataFrame;
    public final DataFrame dataFrameLong;

    public Tooth(final Activity activity){
        instance = this;
        //TODO instanciate dataFrameLong
        dataFrameLong = dataFrame = new DataFrame(new String[]{"PH", "Humidity"}, null, true);
        this.activity = activity;
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
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.e("Smartooth adress" ,device.getAddress());
                Log.e("Smartooth", "Device found " + device.getName());
                Log.e("Smartooth", "With UUIDS " + device.getUuids());

                if(!Config.TOOTH_MACs.contains(device.getAddress())) {
                    Log.d("vertigo", (device.getAddress()));
                    Log.d("vertigo", Config.TOOTH_MACs.toString());
                    return;
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, "Got a device", Toast.LENGTH_LONG).show();

                    }
                });

                bluetoothAdapter.stopLeScan(this);
                Log.d("Smarttooth", "Stopped scan");

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

}
