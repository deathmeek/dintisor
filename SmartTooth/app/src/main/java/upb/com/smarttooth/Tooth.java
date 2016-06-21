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
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import adrian.upb.smarttooth.R;
import upb.com.smarttooth.Renderers.ToothSettings;

public class Tooth{
    private final ToothWatchDog watchdog;
    private static Activity activity;

    public static Activity getActivity() {
        return activity;
    }

    public static void setActivity(Activity a){
        activity = a;
    }

    public class CharacWrapper{
        boolean write;
        BluetoothGattCharacteristic c;
        public int value;
    }

    public class ToothWatchDog {
        private final long timeout;
        private Thread thread;

        public ToothWatchDog(long timeout) {
            this.timeout = timeout;
        }

        public void reset() {
            try {
                thread.interrupt();
            } catch (Exception e){
                //all is good
            }
        }

        public void start() {
            reset();
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(timeout);
                        Tooth.getInstance().resetBluetooth();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            });
            thread.start();
        }
    }

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

    Set<CharacWrapper> map = new LinkedHashSet<CharacWrapper>();
    BluetoothGattCharacteristic target = null;
    boolean real_time_enabled = false;

    private static Tooth instance;
    public final DataFrame dataFrame;
    BluetoothDevice foundDevice = null;
    BluetoothAdapter.LeScanCallback cb = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d("BluetoothScan", "device found - " + device.getAddress() + ", "
                    + device.getName() + ", " + device.getUuids());

            if (foundDevice != null) {
                Log.d("BluetoothScan", "device already found");
                return;
            }

            if (!Config.TOOTH_MACs.contains(device.getAddress())) {
                Log.d("BluetoothScan", "device not in list - " + Config.TOOTH_MACs.toString());
                return;
            }

            foundDevice = device;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "Got a device " + device.getAddress(), Toast.LENGTH_LONG).show();
                }
            });

            bluetoothAdapter.stopLeScan(this);

            bluetoothGatt = device.connectGatt(getActivity(), false, btleGattCallback);
        }
    };

    public void resetBluetooth() {
        try {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        } catch (Exception e) {

        }
        Log.i("Smartooth", "Scanning started");
        foundDevice = null;
        bluetoothAdapter.startLeScan(cb);
    }

    private Tooth(final Activity activity) {
        instance = this;
        this.watchdog = new ToothWatchDog(5000);
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
        // start a thread to schedule reads
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(Config.READ_INTERVAL);
                        if (bluetoothGatt != null) {
                            if(real_time_enabled) {
                                if (phCharac != null) {
                                    enqueueRead(phCharac);
                                }
                                if (humCharac != null) {
                                    enqueueRead(humCharac);
                                }
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
            //watchdog.reset();
            System.out.println("Characteristic " + getName(characteristic.getUuid().toString()) + "was written ");
            CharacWrapper dr = null;
            for (CharacWrapper cw : map) {
                if (cw.c == characteristic && cw.write) {
                    dr = cw;
                }
            }
            map.remove(dr);
            scheduleNext();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            // this will get called anytime you perform a read or write characteristic operation
            //watchdog.reset();
            CharacWrapper dr = null;
            for(CharacWrapper cw : map){
                if(cw.c == characteristic && !cw.write){
                    dr = cw;
                }
            }
            map.remove(dr);
            int value;
            if (characteristic == phCharac) {
                value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                dataFrame.update(value, DataFrame.DataType.PH);
            } else if (characteristic == humCharac) {
                value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                dataFrame.update(value, DataFrame.DataType.Humidity);
            } else {
                if (characteristic == STCharac) {
                    value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                } else {
                    value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
                }
                updateUI(characteristic, value);
            }
            System.out.println("Value is " + value);
            scheduleNext();
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e("Smartooth", "Gatt found " + gatt);
            Tooth.online = newState == BluetoothProfile.STATE_CONNECTED;
            if (Tooth.online) {
                gatt.discoverServices();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ToothSettings.status.setText("Connected");
                        } catch (Exception e) {

                        }
                    }
                });

            } else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ToothSettings.status.setText("Dropped");
                        } catch (Exception e) {

                        }
                    }
                });
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
                            System.out.println(characteristic.getUuid().toString());
                            STCharac = characteristic;
                        }
                        if (characteristic.getUuid().toString().contains(Config.TOOTH_UUID_OUT_V)) {
                            System.out.println("Found V");
                            VCharac = characteristic;
                        }
                    }
                }
            }
            readAllCharac();
        }

        private void readAllCharac() {
            enqueueRead(R.id.button_start);
            enqueueRead(R.id.editText_Voltage);
            enqueueRead(R.id.numberPickerT1);
            enqueueRead(R.id.numberPickerT2);
            enqueueRead(R.id.numberPickerT3);
            enqueueRead(R.id.numberPickerT4);
            enqueueRead(R.id.numberPickerTA);
            enqueueRead(R.id.numberPickerTP);
            enqueueRead(R.id.numberPickerTT);
        }

    };

    private void updateUI(BluetoothGattCharacteristic characteristic, int value) {
        int id = remap(characteristic);
        ToothSettings.getInstance().update(id, value);
    }

    private void scheduleNext() {

        synchronized (map) {
            Iterator<CharacWrapper> it = map.iterator();
            if (it.hasNext()) {
                CharacWrapper c = it.next();
                if (c.write) {
                    bluetoothGatt.writeCharacteristic(c.c);
                } else {
                    bluetoothGatt.readCharacteristic(c.c);
                }
                //this.watchdog.start();
            }
        }
    }

    private String getName(String UUID) {
        if (UUID.equals(Config.TOOTH_UUID_OUT_ST)) {
            return "Start";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_V)) {
            return "Voltage";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_T1)) {
            return "T1";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_T2)) {
            return "T2";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_T3)) {
            return "T3";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_T4)) {
            return "T4";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_TA)) {
            return "TA";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_TT)) {
            return "TT";
        }
        if (UUID.equals(Config.TOOTH_UUID_OUT_TP)) {
            return "TP";
        }
        return null;
    }

    private int remapFormat(int id) {
        if (id != R.id.button_start) {
            return BluetoothGattCharacteristic.FORMAT_UINT32;
        }
        return BluetoothGattCharacteristic.FORMAT_UINT8;
    }
    public void enqueueWrite(int id, int val) {
        BluetoothGattCharacteristic charac = remap(id);
        Log.e("ceva", "enqueueWrite " + val);
        synchronized (map) {
            charac.setValue(val, remapFormat(id), 0);
            if (map.isEmpty()) {
                target = charac;
                bluetoothGatt.writeCharacteristic(charac);
                //this.watchdog.start();
            }
            CharacWrapper cw = new CharacWrapper();
            cw.c = charac;
            cw.write = true;
            map.add(cw);
        }
    }


    private void enqueueRead(int id) {
        BluetoothGattCharacteristic charac = remap(id);
        enqueueRead(charac);
    }
    private void enqueueRead(BluetoothGattCharacteristic charac) {
        synchronized (map) {
            if (map.isEmpty()) {
                target = charac;
                bluetoothGatt.readCharacteristic(charac);
                //this.watchdog.start();
            }
            CharacWrapper cw = new CharacWrapper();
            cw.c = charac;
            cw.write = false;
            map.add(cw);
        }
    }
    private BluetoothGattCharacteristic remap(int id) {
        switch (id) {
            case R.id.button_start: {
                return STCharac;
            }
            case R.id.editText_Voltage: {
                return VCharac;
            }
            case R.id.numberPickerT1: {
                return T1Charac;
            }
            case R.id.numberPickerT2: {
                return T2Charac;
            }
            case R.id.numberPickerT3: {
                return T3Charac;
            }
            case R.id.numberPickerT4: {
                return T4Charac;
            }
            case R.id.numberPickerTA: {
                return TACharac;
            }
            case R.id.numberPickerTP: {
                return TPCharac;
            }
            case R.id.numberPickerTT: {
                return TTCharac;
            }
            default:
                return null;
        }
    }

    private int remap(BluetoothGattCharacteristic c) {
        if (c == STCharac) {
            return R.id.button_start;
        }
        if (c == VCharac) {
            return R.id.editText_Voltage;
        }
        if (c == T1Charac) {
            return R.id.numberPickerT1;
        }
        if (c == T2Charac) {
            return R.id.numberPickerT2;
        }
        if (c == T3Charac) {
            return R.id.numberPickerT3;
        }
        if (c == T4Charac) {
            return R.id.numberPickerT4;
        }
        if (c == TACharac) {
            return R.id.numberPickerTA;
        }
        if (c == TPCharac) {
            return R.id.numberPickerTP;
        }
        if (c == TTCharac) {
            return R.id.numberPickerTT;
        }
        return -1;
    }
}

